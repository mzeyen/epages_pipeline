#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")

// VMs
String vSphereVm = "always_online"
String testVmName = "to-install-debian8-03"

String snapshotName = "OS_and_Puppet"
String installConf = "simple/install-epages.conf"
String envType = "install"
String envIdentifier = "singlehost"
String envOs = "debian"

String excludeFiles = "XML/ShopTypes"

/**
 * vSphere Plugin:
 * 1. Revert to Snapshot.
 * 2. Power-on/Resume VM.
 */
node(vSphereVm) {
  vCenter.revertToSnapshot(testVmName, snapshotName)
  vCenter.powerOnVm(testVmName)
}

/**
 * Install - Singlehost - Debian 8:
 * 1. Deploy epages-infra.
 * 2. Install.
 * 3. Create Fingerprint.
 * 4. Copy Fingerprint.
 * 5. Test Short Urls
 */
node(testVmName) {
  configure.setTime(envOs)
  epagesInfra.deployEpagesInfra()
  install.installEpagesFromVersion(installConf, version)
  fingerprint.createFingerprint(excludeFiles)
  fingerprint.copyFingerprint(version, repo, envIdentifier, envType)
  test.testShortUrls()
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVmName);
}
