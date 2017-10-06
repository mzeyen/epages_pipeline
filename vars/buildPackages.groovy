#!/usr/bin/env groovy

/**
 * Change the version number for ePagesJ.
 */
def changeVersionNumberEpagesJ() {

  stage('Change the version number for ePagesJ') { 
    sh """#!/bin/bash -xe
      ~/epages-infra/scripts/epages/util/change_version_numbers.sh -j EPAGES
    """
  }
}

/**
 * Change the version numbers for ePages 6.
 *
 *@param version String with epages version.
 *@param jiraId String with Jira ID.
 */
def changeVersionNumbers(String version, String jiraId) {

  stage('Change the epages version numbers') { 
    sh """#!/bin/bash -xe
      ~/epages-infra/scripts/epages/util/change_version_numbers.sh -n ${version} -i ${jiraId} -j ${version}
    """
  }
}

/**
 * Create the cdp properties file.
 */
def getPropertiesFromFile() {

  stage('Create cdp properties') { 
    sh"""#!/bin/bash -xe
      ~/epages-infra/scripts/epages/util/get_properties_from_variables.sh -f ~/epages-infra/variables -p ${pwd()}/cdp.properties
      . ${pwd()}/cdp.properties
      export \$(awk -F= '/^[a-zA-Z_].*=/{print \$1}' ${pwd()}/cdp.properties)
    """
  }
}

/**
 * Build patch files for this version.
 */
def buildPatchFiles(String credentialsId) {

  stage('Build patch files') {
       withCredentials([[
	    $class: 'UsernamePasswordMultiBinding',
	    credentialsId: credentialsId,
	    passwordVariable: 'ACCESS_TOKEN',
	    usernameVariable: 'GITHUB_ACCOUNT']]) {
	  	 sh"""#!/bin/bash -xe
    	  	 export GITHUB_ACCOUNT=${GITHUB_ACCOUNT}
    	  	 export ACCESS_TOKEN=${ACCESS_TOKEN}
            ~/epages-infra/scripts/epages/build/build_patch_files.sh -p  ${pwd()}/cdp.properties
        """
		}
   }
}

/**
 * Export all variables to cdp.properties.
 */
def exportCdpProperties() {

  stage('Export all variables to cdp.properties') {
    sh"""#!/bin/bash -x
      .  ${pwd()}/cdp.properties
      export \$(awk -F= '/^[a-zA-Z_].*=/{print \$1}'  ${pwd()}/cdp.properties)
    """
   }
}

/**
 * Build packages for this version.
 */
def buildPackages(String buildDate, String buildVersion) {

  stage('Build packages') { 
    sh"""#!/bin/bash -xe
      ~/epages6-packages/scripts/BuildPackage.sh --version ${buildVersion} --package all --repos ~build/repo/${buildVersion}/${buildDate}
      echo \"REPO=${buildVersion}/${buildDate}\" >>  ${pwd()}/cdp.properties
    """
  }
}

/**
 * Get latest epages6-packages.
 */
def deployEpages6Packages() {

  stage("Get the latest source from epages6-packages repo.") {
    sh"""#!/bin/bash -xe
      if [ ! -d ~/epages6-packages/.git ] ; then
        cd ~
        git clone git@github.com:ePages-de/epages6-packages.git
      else
        cd ~/epages6-packages && git pull origin master
      fi
    """
  }
}

/**
 * Run clone-repo.sh on ftp.epages.com
 *
 *@param version String with the epages version.
 */
def cloneRepoOnFtp(String version) {

  stage('Clone repo on ftp.epages.com') {
    sh"""#!/bin/bash -xe
      ssh -i ~/.ssh/keys/ep-build-id_rsa ftp.epages.com \"~/epages-infra/scripts/epages/util/clone-repo.sh -n ${version}\"
    """
   }
}

/**
 * Run clone-repo.sh.
 *
 *@param version String with the epages version.
 */
def cloneRepo(String version) {

  stage('Clone repo.') {
    sh"""#!/bin/bash -xe
      ~/epages-infra/scripts/epages/util/clone-repo.sh -n ${version}
    """
   }
}
