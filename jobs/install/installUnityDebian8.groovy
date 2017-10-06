#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "to-install-debian8-01"

String snapshotName = "OS"
String installConf = "simple/install-epages-unity.conf"
String envType = "install"
String envIdentifier = "unity"
String envOs = "debian"

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
 * Install - Singlehost - Debian 8:
 * 1. Deploy epages-infra.
 * 2. Install.
 * 3. Create shops.
 * 4. Create Fingerprint.
 * 5. Copy Fingerprint.
 */
node(testVm) {
  configure.setTime(envOs)
  epagesInfra.deployEpagesInfra()
  install.installEpagesFromVersion(installConf, version)
  configure.createUnityShops()
  fingerprint.createFingerprint()
  fingerprint.copyFingerprint(version, repo, envIdentifier, envType)
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
