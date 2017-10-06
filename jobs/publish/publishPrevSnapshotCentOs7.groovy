#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-patch-sh-centos7-01"
String testVmFqdn = "cd-vm-test-patch-sh-centos7-01.intern.epages.de"

String snapshotName = "ci_source"
String installConf = "simple/install-epages-BRUS.conf"

/**
 * vSphere Plugin:
 * 1. Revert to Snapshot.
 * 2. Power-on/Resume VM.
 */
node(vSphereVm) {
  vCenter.revertToSnapshot(testVmFqdn, snapshotName)
  vCenter.powerOnVm(testVmFqdn)
}

/**
 * Install - Normal - Singlehost - CentOS 6:
 * 1. Deploy epages-infra
 * 3. Install.
 * 4. Configure dialects.
 */
node(testVm) {
  epagesInfra.deployEpagesInfra()
  install.installEpagesFromVersion(installConf, version)
  configure.dialects()
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 * 2. Create snapshot.
 * 3. Delete previous snapshot.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm)
  vCenter.createSnapshot(testVmFqdn, version)
  vCenter.deleteSnapshot(testVmFqdn, previousVersion)
}
