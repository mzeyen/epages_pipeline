#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// VMs
String vSphereVm = "always_online"
String testVmSite = "cd-vm-test-install-flex-site-centos-01"
String testVmStore = "cd-vm-test-install-flex-store-centos-01"

String snapshotName = "OS"
String envType = "install"
String envIdentifier = "flex"
String excludeFiles = "'XML/ShopTypes'"
String repo_url = "ftp://ftp.epages.com/pub/epages/build/repo/${version}/latest"


/**
 * vSphere Plugin:
 * 1. Revert to Snapshot.
 * 2. Power-on/Resume VM.
 */
node(vSphereVm) {
  vCenter.revertToSnapshot(testVmSite, snapshotName)
  vCenter.powerOnVm(testVmSite)
  vCenter.revertToSnapshot(testVmStore, snapshotName)
  vCenter.powerOnVm(testVmStore)
}

/**
 * Create a Flex Management Host:
 * 1. Checkout epages-infra.
 * 2. Update Mirrors
 * 3. Install epages-release package. 
 * 4. Install ansible.
 * 5. Update Flex conf.
 * 6. Check connection to FlexStore VM.
 * 7. Install Flex on FlexStore VM.
 */
node(testVmSite) {
  epagesInfra.deployEpagesInfra()
  configure.updateMirrors()
  install.installEpagesReleaseFromVersion(version)
  install.installAnsible()
  configure.updateConf(repo_url, testVmStore)
  configure.checkFlexStoreConnection()
  install.installFlexStore(testVmStore)
}

/**
 * Install - Normal - Flex - CentOs ?: 
 * 1. Create FlexTestShop.
 * 2. Create Fingerprint.
 * 3. Copy Fingerprint.
 */
node(testVmStore) {
  epagesInfra.deployEpagesInfra()
  fingerprint.createFingerprint(excludeFiles)
  fingerprint.copyFingerprint(version, repo, envIdentifier, envType)
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 * 2. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVmSite);
  vCenter.powerOffVm(testVmStore);
}
