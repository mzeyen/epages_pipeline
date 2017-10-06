#!/usr/bin/env groovy

// Shared library
library 'jenkins-rnd-pipeline'

// Job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "dbranch-vm-patch-ms-centos-01"

String snapshotName = previousVersion

// Fingerprint
String envIdentifier = "multistores"
String envType = "patch"
String excludeFiles = "'mysql_config.txt DBSchema/GlobalVariables XML/ShopTypes'"
String envOs = "centos"

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
 * Patch - Multistore - Singlehost - CentOS 6:
 *
 * 1. Checkout epages-infra.
 * 2. Patch.
 * 3. Add multistore cartridge to Business Unit.
 * 4. Create Fingerprint.
 * 5. Copy Fingerprint.
 */
node(testVm) {
  configure.setTime(envOs)
  epagesInfra.deployEpagesInfra()
  patch.patch(version)
  configure.addCartridgesToBu("Master", "'DE_EPAGES::MultiStoreMasterConnector'")
  fingerprint.createFingerprint(excludeFiles)
  fingerprint.copyFingerprint(version, repo, envIdentifier, envType)
}

/**
 * vSphere Plugin:
 *
 * 1. Power-off/Resume Vm.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
