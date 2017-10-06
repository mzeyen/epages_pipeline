#!/usr/bin/env groovy

// Shared library
library 'jenkins-rnd-pipeline'

// Job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "qa-vm-lin64-2"
String testVmFqdn = "qa-vm-lin64-2.intern.epages.de"
String snapshotName = previousVersion

// Fingerprint
String envOs = "centos"
String envType = "patch_history"
String envIdentifier = "singlehost"
String configuration = "~/epages-infra/configuration/install-epages/simple/install-epages.conf"
String excludeFiles = "'mysql_config.txt DBSchema/GlobalVariables DBSchema/Columns DBSchema/KeyColumnUsage DBSchema/NonUniqueIndexes DBSchema/Tables XML/DemoShop XML/ShopTypes'"

// ESF parameters.
esfParameter = [
  new StringParameterValue("TESTGROUPS", "SMOKETEST"),
  new StringParameterValue("SHOP", "DemoShop"),
  new StringParameterValue("TARGET_DOMAIN", "qa-vm-lin64-2.intern.epages.de"),
  new StringParameterValue("VERSION", version),
  new StringParameterValue("REPO", repo),
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
  vCenter.revertToSnapshot(testVmFqdn, snapshotName)
  vCenter.powerOnVm(testVmFqdn)
}

/**
 * Patch - History - Singlehost - CentOS 6:
 *
 * 1. Checkout epages-infra.
 * 2. Patch.
 * 3. Install additional Cartridges.
 * 4. Create Fingerprint.
 * 5. Copy Fingerprint.
 * 6. Run ESF tests.
 */
node(testVm) {
  configure.setTime(envOs)
  epagesInfra.deployEpagesInfra()
  patch.patch(version)
  install.installAdditionalCartridges(configuration)
  fingerprint.createFingerprint(excludeFiles)
  fingerprint.copyFingerprint(version, repo, envIdentifier, envType)
  esf.runEsfTests(esfParameter)
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVmFqdn);
}
