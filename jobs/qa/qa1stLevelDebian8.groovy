#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-master-07"
String snapshotName = "OS"

String installConf = "simple/install-epages.conf"

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
 * Install - 1st level tests - Singlehost - Debian 8:
 * 1. Deploy epages-infra
 * 4. Install.
 * 5. Run regression tests.
 * 6. Test perl modules.
 * 7. Compile templates.
 * 8. Test for unused files.
 * 9. Test Languages.
 * 10. Test Languages.
 * 11. Test create on shop per shoptype.
 * 12. Test short URLs
 */
node(testVm) {
  epagesInfra.deployEpagesInfra()
  install.installEpagesFromVersion(installConf, version)
  test.regressionTests()
  test.testPerl("Site")
  test.compileTemplatesTest()
  test.checkUnusedFiles()
  test.checkLanguageTags("Store")
  test.checkLanguageTags("Site")
  test.createShopPerShoptype()
  test.runCompileTestPerlFilesTest()
  test.testShortUrls()
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
