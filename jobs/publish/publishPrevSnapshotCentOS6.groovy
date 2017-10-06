#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-patch-sh-centos-01"
String snapshotName = "OS"
String installConf = "simple/install-epages.conf"

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
 * Install - Singlehost - CentOS 6:
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
  vCenter.powerOffVm(testVm)
  vCenter.createSnapshot(testVm, version)
  vCenter.deleteSnapshot(testVm, previousVersion)
}
