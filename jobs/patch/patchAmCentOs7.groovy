#!/usr/bin/env groovy

// Shared library
library 'jenkins-rnd-pipeline'

// Job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-patch-008"
String testVmFqdn = "cd-vm-test-patch-008.intern.epages.de"

String snapshotName = previousVersion
String configuration = "~/epages-infra/configuration/install-epages/simple/install-epages-i18n.conf"
String envIdentifier = "singlehost-am"
String envType = "patch"
String envOs = "centos"

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
 * Patch - AM like - Singlehost - ?:
 *
 * 1. Checkout epages-infra.
 * 2. Patch like AM.
 * 3. Install additional Cartridges.
 * 4. Create Fingerprint.
 * 5. Copy Fingerprint.
 */
node(testVm) {
  configure.setTime(envOs)
  epagesInfra.deployEpagesInfra()
  patch.patchAmLike(version)
  install.installAdditionalCartridges(configuration)
  fingerprint.createFingerprint()
  fingerprint.copyFingerprint(version, repo, envIdentifier, envType)
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVmFqdn);
}
