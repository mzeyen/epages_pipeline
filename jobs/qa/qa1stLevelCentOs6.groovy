#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String repo = getProperty("REPO")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-master-03"
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
 * Install - 1st level tests - Singlehost - CentOS6:
 * 1. Deploy epages-infra
 * 2. Install.
 * 3. Run regression tests.
 * 4. Test perl modules.
 * 5. Compile templates.
 * 6. Test for unused files.
 * 7. Test Languages.
 * 8. Test Languages.
 * 9. Test create on shop per shoptype.
 * 10. Test short URLs
 */
node(testVm) {
  epagesInfra.deployEpagesInfra()
  install.installEpagesFromRepo(installConf, repo)
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
