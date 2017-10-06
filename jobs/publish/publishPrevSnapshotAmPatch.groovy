#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-patch-008.intern.epages.de"
String snapshotName = "OS_with_puppet"
String installConf = "simple/install-epages-i18n.conf"

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
 * Install - AM - Debian 8:
 * 1. Deploy epages-infra
 * 2. Install.
 */
node(testVmName) {
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
  vCenter.powerOffVm(testVm)
  vCenter.createSnapshot(testVm, version)
  vCenter.deleteSnapshot(testVm, previousVersion)
}
