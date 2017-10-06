#!/usr/bin/env groovy

// Shared library
library 'jenkins-rnd-pipeline'

// VMs
String testVm = "ep-build-64"

// Access token for github to build patch files
String credentialsId="a17f6d70-86d1-4a6e-96d1-f7020aff585d"

/**
 * 1. Deploy epages-infra.
 * 2. Run change_version_numbers.sh.
 * 3. Run get_properties_from_variables.sh.
 * 4. Build patch files.
 * 5. Deploy epages6-packages repo.
 * 6. Get current date and current verions.
 * 7. Build packages.
 * 8. Set build parameters for doqwnstream jobs.
 * 9. Run unity test wrapper.
 * 10. Run install wrapper.
 * 11. Run patch wrapper
 */
node(testVm) {
  epagesInfra.deployEpagesInfra()
  buildPackages.changeVersionNumberEpagesJ()
  buildPackages.getPropertiesFromFile()
  buildPackages.exportCdpProperties()
  buildPackages.buildPatchFiles(credentialsId)
  buildPackages.deployEpages6Packages()

  //Create variables for date and version
  String dateString = sh(script: 'echo $(date +%Y.%m.%d-%H.%M.%S)', returnStdout: true)
  String date = dateString.trim()
  String versionString = sh(script: 'echo $(awk -F= "/^CURRENT_EPAGES_VERSION_NUMBER=/{print \$2}" ~/epages-infra/variables)', returnStdout: true)
  String version = versionString.minus("CURRENT_EPAGES_VERSION_NUMBER=").trim()
  String previousVersionString = sh(script: 'echo $(awk -F= "/^PREVIOUS_EPAGES_VERSION_NUMBER=/{print \$2}" ~/epages-infra/variables)', returnStdout: true)
  String previousVersion = previousVersionString.minus("PREVIOUS_EPAGES_VERSION_NUMBER=").trim()
  String repo = version + "/" + date

  buildPackages.buildPackages(date, version)

  //Build parameters
  def buildParameters = [
      new StringParameterValue("REPO", repo),
      new StringParameterValue("VERSION", version),
      new StringParameterValue("PREVIOUS_VERSION", previousVersion)
    ]

  def wrapperJobs = [
    "install/installWrapper",
    "patch/patchWrapper",
    "qa/qaWrapper"
  ]
  
  def parallelJobs = [:]

  for (int i = 0; i <  wrapperJobs.size(); i++) {
    // Because of reasons: http://blog.freeside.co/2013/03/29/groovy-gotcha-for-loops-and-closure-scope/
    def index = i
    parallelJobs["wrapper${i}"] = {
        
      stage("Start Job:" + wrapperJobs[index]) {
        build job: wrapperJobs[index],
              wait: false,
              parameters: buildParameters
      }
    }
  }
  parallel parallelJobs
}