#!/usr/bin/env groovy

// Shared library
library 'jenkins-rnd-pipeline'

// Job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-patch-004"

String snapshotName = previousVersion

// Fingerprint
String envOs = "debian"
String envType = "patch"
String envIdentifier = "singlehost"
String configuration = "~/epages-infra/configuration/install-epages/simple/install-epages.conf"
String excludedFiles = "'mysql_config.txt DBSchema/GlobalVariables XML/ShopTypes'"

// ESF parameters.
esfParameter = [
  new StringParameterValue("TESTGROUPS", "SMOKETEST"),
  new StringParameterValue("SHOP", "DemoShop"),
  new StringParameterValue("VERSION", version),
  new StringParameterValue("REPO", repo),
  new StringParameterValue("TARGET_DOMAIN", "cd-vm-test-patch-004.intern.epages.de"),
  new StringParameterValue("ENV_OS", envOs),
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
 * Patch - Normal - Singlehost - Debian 7:
 *
 * 1. Checkout epages-infra.
 * 2. Patch.
 * 3. Install additional Cartridges.
 * 4. Create Fingerprint.
 * 5. Copy Fingerprint.
 * 7. Configure app server.
 * 8. Stop epages cron jobs.
 * 6. Run esf tests.
 */
node(testVm) {
  configure.setTime(envOs)
  epagesInfra.deployEpagesInfra()
  patch.patch(version)
  install.installAdditionalCartridges(configuration)
  fingerprint.createFingerprint(excludedFiles)
  fingerprint.copyFingerprint(version, repo, envIdentifier, envType)
  configure.configureAppServers("390", "3")
  configure.stopCronJobs()
  esf.runEsfTests(esfParameter)
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
