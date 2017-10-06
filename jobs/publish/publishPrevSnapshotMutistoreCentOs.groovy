#!/usr/bin/env groovy

// Shared library
library 'jenkins-rnd-pipeline'

// Job parameter
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

String vSphereVm = "always_online"
String testVm = "dbranch-vm-patch-ms-centos-01"
String snapshotName = "OS"
String installConf = "simple/install-multistores.conf"
String store = "Master"
String cartridge = "DE_EPAGES::MultiStoreMasterConnector"

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
 * Install - Multistores - CentOS 6:
 * 1. Deploy epages-infra.
 * 2. Update Mirrors.
 * 3. Install.
 * 4. Add cartridge to Business unit.
 */
node(testVm) {
  epagesInfra.deployEpagesInfra()
  configure.updateMirrors()
  install.installEpagesFromVersion(installConf, version)
  configure.addCartridgesToBu(store, cartridge)
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
