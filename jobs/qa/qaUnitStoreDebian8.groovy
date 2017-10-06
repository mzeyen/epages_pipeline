#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String repo = getProperty("REPO")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-master-06"
String snapshotName = "OS"

String envOs = "debian"

String installConf = "simple/install-epages-250MB.conf"

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
 * Install - Run unit tests on Store - Debian 7:
 * 1. Get actual epages-infra
 * 2. Install.
 * 3. Test Sonar.
 */
node(testVm) {
  configure.setTime(envOs)
  epagesInfra.deployEpagesInfra()
  install.installEpagesFromRepo(installConf, repo)
  test.testSonar()
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
