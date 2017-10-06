#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-install-sh-centos7-01.intern.epages.de"
String testVmName = "cd-vm-test-install-sh-centos7-01"

String snapshotName = "ci_source"
String installConf = "simple/install-epages-BRUS.conf"
String envType = "install"
String envIdentifier = "singlehost"
String envOs = "centos"

String excludeFiles = "'mysql_config.txt DBSchema/GlobalVariables XML/ShopTypes'"

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
 * Install - Normal - Singlehost - CentOS 6:
 * 1. Run Puppet Agent Test.
 * 2. Update Mirrors.
 * 3. Install.
 * 4. Configure dialects.
 * 4. Create Fingerprint.
 * 5. Copy Fingerprint.
 * 6. Test Languages.
 * 7. Test Languages.
 * 8. Test object reference test.
 * 9. Test short URLs
 */
node(testVmName) {
  configure.setTime(envOs)
  puppetAgent.test()
  configure.updateMirrors()
  install.installEpagesFromVersion(installConf, version)
  configure.dialects()
  fingerprint.createFingerprint(excludeFiles)
  fingerprint.copyFingerprint(version, repo, envIdentifier, envType)
  test.checkLanguageTest()
  test.compareLanguageFiles()
  //test.checkDuplicatedLanguageTags()
  test.checkObjectReferenceTest()
  //test.testShortUrls()
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
