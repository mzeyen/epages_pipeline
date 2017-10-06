#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-patch-sh-centos-01"

String snapshotName = previousVersion

// Fingerprint
String envType = "patch"
String envIdentifier = "singlehost"
String excludeFiles = "'mysql_config.txt DBSchema/GlobalVariables DBSchema/Columns DBSchema/KeyColumnUsage DBSchema/NonUniqueIndexes DBSchema/Tables XML/DemoShop XML/ShopTypes'"
String repo_url = "ftp://ftp.epages.com/pub/epages/build/repo/${version}/latest"
String configuration = "~/epages-infra/configuration/install-epages/simple/install-epages.conf"
String envOs = "centos"

// ESF parameters.
esfParameter = [
  new StringParameterValue("TESTGROUPS", "SMOKETEST,LANGUAGE"),
  new StringParameterValue("SHOP", "DemoShop"),
  new StringParameterValue("TARGET_DOMAIN", "cd-vm-test-patch-sh-centos-01.intern.epages.de"),
  new StringParameterValue("REPO", repo),
  new StringParameterValue("VERSION", version),
  new StringParameterValue("ENV_OS", "centos"),
  new StringParameterValue("ENV_TYPE", envType),
  new StringParameterValue("ENV_IDENTIFIER", envIdentifier)]

/**
 * vSphere Plugin:
 *
 * 1. Revert to Snapshot.
 * 2. Power-on/Resume VM.
 */
node(vSphereVm) {
  vCenter.revertToSnapshot(testVm, snapshotName)
  vCenter.powerOnVm(testVm)
}

/**
 * Patch - Normal - Singlehost - CentOS 6:
 *
 * 1. Checkout epages-infra.
 * 2. Patch.
 * 3. Install additional Cartridges.
 * 4. Create Fingerprint.
 * 5. Copy Fingerprint.
 * 6. Install language packs.
 * 7. Configure app server.
 * 8. Stop epages cron jobs.
 * 9. Run esf tests.
 */
node(testVm) {
  configure.setTime(envOs)
  epagesInfra.deployEpagesInfra()
  patch.patch(version)
  install.installAdditionalCartridges(configuration)
  fingerprint.createFingerprint(excludeFiles)
  fingerprint.copyFingerprint(version, repo, envIdentifier, envType)
  install.installLanguagePacks("fr")
  configure.configureAppServers("390", "3")
  configure.stopCronJobs()
  esf.runEsfTests(esfParameter)
}

/**
 * vSphere Plugin:
 *
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
