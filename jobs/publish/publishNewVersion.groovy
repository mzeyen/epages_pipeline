#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")
String snapshotName = previousVersion

// "publishTest*" jobs parameter.
def parameter = [
  new StringParameterValue("PREVIOUS_VERSION", previousVersion),
  new StringParameterValue("VERSION", version)
]

// VMs
String vSphereVm = "always_online"
String buildVm = "ep-build-64"

/**
 * 1. Clone epages6-packages repo.
 * 2. Clone epages-infra repo.
 * 3. Publish new repo.
 * 4. Delete repos.
 * 5. Delete repos on remote VM.
 * 6. Tag repos after publish.
 * 7. Run tests after publish.
 */
node(buildVm) {
  gitActions.cloneFromBranch("ePages-de", "epages6-packages", "master")
  gitActions.cloneFromBranch("ePages-de", "epages-infra", "master")
  publish.publishRepo(version)
  publish.deleteRepo(version)
  publish.deleteRepo(version, "ftp.epages.com")
  publish.tagGitAfterPublish(version)
  publish.runTestsAfterPublish(parameter)
}
