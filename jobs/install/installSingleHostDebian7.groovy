#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "ci-vm-install-test-debian-latest-01"

String snapshotName = "OS"
String installConf = "simple/install-epages.conf"
String envType = "install"
String envIdentifier = "singlehost"
String envOs = "debian"

String excludeFiles = "'mysql_config.txt DBSchema/GlobalVariables XML/ShopTypes'"

/**
 * vSphere Plugin:
 * 1. Revert to Snapshot.
 * 2. Power-on/Resume VM.
 */
node(vSphereVm) {
  vCenter.revertToSnapshot(testVm, snapshotName)
  vCenter.powerOnVm(testVm)
}

/**
 * Install - Singlehost - Debian 7:
 * 1. Deploy epages-infra.
 * 2. Install.
 * 3. Create Fingerprint.
 * 4. Copy Fingerprint.
 */
node(testVm) {
  configure.setTime(envOs)
  epagesInfra.deployEpagesInfra()
  install.installEpagesFromVersion(installConf, version)
  fingerprint.createFingerprint(excludeFiles)
  fingerprint.copyFingerprint(version, repo, envIdentifier, envType)
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm)
}
