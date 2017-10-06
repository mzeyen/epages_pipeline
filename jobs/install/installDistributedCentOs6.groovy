#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")

// VMs
String vSphereVm = "always_online"

String testVmAsjs = "dbranch-vm-install-di-asjs-centos-01"
String envIdentifierAsjs = "distributed_three_hosts-asjs"

String testVmDb = "dbranch-vm-install-di-db-centos-01"
String envIdentifierDb = "distributed_three_hosts-db"

String testVmWsrr = "dbranch-vm-install-di-wsrr-centos-01"
String testVmWsrrName = "dbranch-vm-install-di-wsrr-centos-01.intern.epages.de"
String envIdentifierWsrr = "distributed_three_hosts-wsrr"

String snapshotName = "OS"
String installConf = "simple/install-epages-distributed.conf"
String envOs = "centos"
String envType = "install"
String envIdentifier = "distributed_three_hosts"

// Fingerprint
String sourceDir = "/tmp/epages-fingerprint"
String excludeFiles1 = "Files/Config"
String excludeFiles2 = "XML/ShopTypes"

esfParameters = [
  new StringParameterValue("TARGET_DOMAIN", testVmWsrrName),
  new StringParameterValue("TESTGROUPS", "SEARCH,CORE"),
  new StringParameterValue("SHOP", "DemoShop"),
  new StringParameterValue("ENV_OS", envOs),
  new StringParameterValue("ENV_TYPE", envType),
  new StringParameterValue("ENV_IDENTIFIER", envIdentifier),
]

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
  configure.setTime(envOs)
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
 * 4. Create fingerprint.
 * 5. Copy fingerprint.
 */
node(testVmDb) {
  configure.setTime(envOs)
  epagesInfra.deployEpagesInfra()
  configure.updateMirrors()
  install.installDistributedFromConf(testVmAsjs, version)
  fingerprint.createFingerprintDistributed(excludeFiles1)
  fingerprint.copyFingerprint(version, repo, envIdentifierDb, envType, sourceDir)
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
 * 4. Create fingerprint.
 * 5. Copy fingerprint.
 */
node(testVmWsrr) {
  configure.setTime(envOs)
  epagesInfra.deployEpagesInfra()
  configure.updateMirrors()
  install.installDistributedFromConf(testVmAsjs, version)
  fingerprint.createFingerprintDistributed(excludeFiles1)
  fingerprint.copyFingerprint(version, repo, envIdentifierWsrr, envType, sourceDir)
}

/**
 * Install - Services - CentOS 6
 * 1. Install services from configuration.
 * 2. Create fingerprint.
 * 3. Copy fingerprint.
 * 4. Stop cron jobs.
 * 5. configure.stopCronJobs()
 */
node(testVmAsjs) {
  configure.setTime(envOs)
  install.finishInstallFileserverFromVersion(version)
  fingerprint.createFingerprintDistributed(excludeFiles2)
  fingerprint.copyFingerprint(version, repo, envIdentifierAsjs, envType, sourceDir)
  configure.stopCronJobs()
  esf.runEsfTests(esfParameters)
}

/**
 * vSphere plugin:
 * 1. Power-off all three VMs.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVmAsjs)
  vCenter.powerOffVm(testVmDb)
  vCenter.powerOffVm(testVmWsrr)
}
