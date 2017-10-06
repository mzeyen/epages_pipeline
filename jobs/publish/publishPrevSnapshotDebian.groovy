#!/usr/bin/env groovy

// Shared library
library 'jenkins-rnd-pipeline'

// Job parameter
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-patch-004"
String snapshotName = "OS"

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
 * Install - Singlehost - Debian 7:
 * 1. Deploy epages-infra.
 * 2. Install.
 */
node(testVm) {
  epagesInfra.deployEpagesInfra()
  install.installEpagesFromVersion(installConf, version)
}


/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 * 2. Create snapshot.
 * 3. Delete previous snapshot.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVmFqdn)
  vCenter.createSnapshot(testVmFqdn, version)
  vCenter.deleteSnapshot(testVmFqdn, previousVersion)
}
