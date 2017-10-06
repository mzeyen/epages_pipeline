#!/usr/bin/env groovy

// Shared library
library 'jenkins-rnd-pipeline'

// Job parameter
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

String vSphereVm = "always_online"
String testVm = "cd-vm-test-patch-unity"
String snapshotName = "ci_source"
String installConf = "simple/install-epages-unity.conf"

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
 * Install - Singlehost Unity - CentOs 7:
 * 1. Deploy epages-infra.
 * 2. Install.
 * 4. Configure unity ssl.
 * 5. Create shops.
 */
node(testVm) {
  configure.setTime(envOs)
  epagesInfra.deployEpagesInfra()
  install.installEpagesFromVersion(installConf, version)
  configure.setupUnitySsl()
  configure.createUnityShops()
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 * 2. Create snapshot.
 * 3. Delete previous snapshot.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm)
  vCenter.createSnapshot(testVm, version)
  vCenter.deleteSnapshot(testVm, previousVersion)
}
