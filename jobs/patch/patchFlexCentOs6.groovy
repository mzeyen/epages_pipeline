#!/usr/bin/env groovy

// Shared library
library 'jenkins-rnd-pipeline'

// Job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

// VMs
String vSphereVm = "always_online"
String testVmSite = "ci-vm-flex-patch-test-site-centos-latest-01"
String testVmSiteFqdn = "ci-vm-flex-patch-test-site-centos-latest-01.intern.epages.de"
String testVmStore = "ci-vm-flex-patch-test-store-centos-latest-01"
String testVmStoreFqdn = "ci-vm-flex-patch-test-store-centos-latest-01.intern.epages.de"
String snapshotNameSiteVm = "OS"
String snapshotName = previousVersion

// Fingerprint
String envType = "patch"
String envIdentifier = "flex"
String excludeFiles = "'XML/ShopTypes'"
String repoUrl = "ftp://ftp.epages.com/pub/epages/build/repo/${version}/latest"

// ESF parameters.
esfParameter = [
  new StringParameterValue("TESTGROUPS", "SMOKETEST"),
  new StringParameterValue("SHOP", "FlexShop"),
  new StringParameterValue("SHOPTYPE", "Flex"),
  new StringParameterValue("TARGET_DOMAIN", "ci-vm-flex-patch-test-store-centos-latest-01.intern.epages.de"),
  new StringParameterValue("REPO", repo),
  new StringParameterValue("VERSION", version),
  new StringParameterValue("ENV_OS", "centos"),
  new StringParameterValue("ENV_TYPE", envType),
  new StringParameterValue("ENV_IDENTIFIER", envIdentifier)]

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
 * Short description for this job (ToDo.) 
 *
 * 1. Checkout epages-infra.
 * 2. Update Mirrors
 * 3. Install epages-release package. 
 * 4. Install ansible.
 * 4. Write FlexStore Vm to config file.
 * 6. Check connection to FlexStore.
 * 7. Patch FlexStore VM.
 */
node(testVmSite) {
  epagesInfra.deployEpagesInfra()
  configure.updateMirrors()
  install.installEpagesReleaseFromVersion(version)
  install.installAnsible()
  configure.updateConf(repoUrl, testVmStore)
  configure.checkFlexStoreConnection()
  patch.patchFlexStore(testVmStore, version)
}

/**
 * Shot description for this job (ToDo.) 
 *
 * 1. Checkout epages-infra.
 * 2. Create fingerprint.
 * 3. Copy fingerprint.
 * 4. Run ESF tests.
 */
node(testVmStore) {
  epagesInfra.deployEpagesInfra()
  fingerprint.createFingerprint(excludeFiles)
  fingerprint.copyFingerprint(version, repo, envIdentifier, envType)
  esf.runEsfTests(esfParameter)
}

/**
 * vSphere Plugin:
 *
 * 1. Power-off/Resume VM.
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVmSite);
  vCenter.powerOffVm(testVmStore);
}
