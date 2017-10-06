#!/usr/bin/env groovy

// Shared library
library 'jenkins-rnd-pipeline'

// Job parameter
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

// VMs
String vSphereVm = "always_online"
String testVmSite = "ci-vm-flex-patch-test-site-centos-latest-01"
String testVmStore = "ci-vm-flex-patch-test-store-centos-latest-01"
String snapshotNameSiteVm = "OS"

/**
 * vSphere Plugin:
 *
 * 1. Revert to Snapshot.
 * 2. Power-on/Resume VM.
 * 3. Revert to Snapshot.
 * 4. Power-on/Resume VM.
 */
node(vSphereVm) {
  vCenter.revertToSnapshot(testVmSite, snapshotNameSiteVm)
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
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 * 2. Create snapshot.
 * 3. Delete previous snapshot.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVmSite)
  vCenter.createSnapshot(testVmSite, version)
  vCenter.deleteSnapshot(testVmSite, previousVersion)
  vCenter.powerOffVm(testVmStore)
  vCenter.createSnapshot(testVmStore, version)
  vCenter.deleteSnapshot(testVmStore, previousVersion)
}
