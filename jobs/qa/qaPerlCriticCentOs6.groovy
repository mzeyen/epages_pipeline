#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String repo = getProperty("REPO")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-master-02"

String installConf = "simple/install-epages.conf"
String snapshotName = "OS"

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
 * Install - Run perl critic tests:
 *
 * 1. Get epages-infra.
 * 2. Get epages cartridges
 * 3. Install.
 * 4. Stop epages services.
 * 5. Copy Cartridges folder.
 * 6. Test perl critic.
 */
node(testVm) {
  epagesInfra.deployEpagesInfra()
  configure.getCartridges()
  install.installEpagesFromRepo(installConf, repo)
  configure.stopEpages()
  configure.copyCartridgesToEpagesCartridges()
  test.testCritic()
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
