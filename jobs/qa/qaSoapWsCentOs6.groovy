#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String version = getProperty("VERSION")
String repo = getProperty("REPO")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-master-05"
String snapshotName = "OS"

String installConf = "simple/install-epages-390MBx6.conf"

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
 * Install - Run soap tests:
 *
 * 1. Get actual epages-infra
 * 2. Install.
 * 3. Test web services.
 */
node(testVm) {
  epagesInfra.deployEpagesInfra()
  install.installEpagesFromRepo(installConf, repo)
  test.testSoap("dev")
}

/**
 * vSphere Plugin:
 *
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
