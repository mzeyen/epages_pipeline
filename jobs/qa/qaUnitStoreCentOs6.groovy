#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-master-01"
String snapshotName = "OS"
String envOs = "centOS"

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
 * Install - Run unit tests on Store:
 * 1. Get actual epages-infra
 * 2. Pupet agent test.
 * 3. Install.
 * 4. Test perl modules on store "Store".
 */
node(testVm) {
  epagesInfra.deployEpagesInfra()
  configure.setTime(envOs)
  puppetAgent.test()
  install.installEpagesFromRepo(installConf, repo)
  test.testPerl("Store")
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
