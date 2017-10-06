#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

// VMs
String vSphereVm = "always_online"
String testVmAsjs = "dbranch-vm-patch-di-asjs-centos-01"
String testVmDb = "dbranch-vm-patch-di-db-centos-01"
String testVmWsrr = "dbranch-vm-patch-di-wsrr-centos-01"
String snapshotName = "OS"

/**
 * vSphere Plugin:
 * 1. Revert to Snapshot.
 * 2. Power-on/Resume VM.
 */
node(vSphereVm) {
  vCenter.revertToSnapshot(testVmAsjs, snapshotName)
  vCenter.powerOnVm(testVmAsjs)
}

/**
 * Install - Fileserver - CentOS 6
 * 1. Deploy epages-infra.
 * 2. Update mirrors.
 * 3. Install the fileserver.
 */
node(testVmAsjs) {
  epagesInfra.deployEpagesInfra()
  configure.updateMirrors()
  install.installFileserverFromVersion(version)
}

/**
 * vSphere plugin:
 * 1. Revert to snapshot.
 * 2. Power-on/Resume VM.
 */
node(vSphereVm) {
  vCenter.revertToSnapshot(testVmDb, snapshotName)
  vCenter.powerOnVm(testVmDb)
}

/**
 * Install - Services - CentOS 6
 * 1. Deploy epages-infra.
 * 2. Update mirrors.
 * 3. Install services from configuration.
 */
node(testVmDb) {
  epagesInfra.deployEpagesInfra()
  configure.updateMirrors()
  install.installDistributedFromConf(testVmAsjs, version)
}

/**
 * vSphere plugin:
 * 1. Revert to snapshot.
 * 2. Power-on/Resume VM.
 */
node(vSphereVm) {
  vCenter.revertToSnapshot(testVmWsrr, snapshotName)
  vCenter.powerOnVm(testVmWsrr)
}

/**
 * Install - Services - CentOS 6
 * 1. Deploy epages-infra.
 * 2. Update mirrors.
 * 3. Install services from configuration.
 */
node(testVmWsrr) {
  epagesInfra.deployEpagesInfra()
  configure.updateMirrors()
  install.installDistributedFromConf(testVmAsjs, version)
}

/**
 * Install - Services - CentOS 6
 * 1. Install services from configuration.
 * 2. Stop cron jobs.
 * 3. configure.stopCronJobs()
 */
node(testVmAsjs) {
  configure.setTime(envOs)
  install.finishInstallFileserverFromVersion(version)
  configure.stopCronJobs()
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VMs.
 * 2. Create snapshots.
 * 3. Delete previous snapshots.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVmAsjs)
  vCenter.createSnapshot(testVmAsjs, version)
  vCenter.deleteSnapshot(testVmAsjs, previousVersion)
  vCenter.powerOffVm(testVmDb)
  vCenter.createSnapshot(testVmDb, version)
  vCenter.deleteSnapshot(testVmDb, previousVersion)
  vCenter.powerOffVm(testVmWsrr)
  vCenter.createSnapshot(testVmWsrr, version)
  vCenter.deleteSnapshot(testVmWsrr, previousVersion)
}
