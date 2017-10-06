#!/usr/bin/env groovy

// Shared library
library 'jenkins-rnd-pipeline'

// job parameter
String version = getProperty("EPAGES_VERSION")
String jiraId = getProperty("JIRA_ID")

String vSpehereVm = "always_online"
String testVm = "ep-build-64"

/**
 * 1. Deploy epages-infra.
 * 2. Run change_version_numbers.sh.
 * 3. Run clone-repo.sh
 * 4. Ron clone-repo.sh on ftp.epges.com.
 */
node(testVm) {
  epagesInfra.deployEpagesInfra()
  configure.changeVersionNumbers(version, jiraId)
  buildPackages.cloneRepo(version)
  buildPackages.cloneRepoOnFtp(version)
}
