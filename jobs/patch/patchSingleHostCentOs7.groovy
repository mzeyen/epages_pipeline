#!/usr/bin/env groovy

// Shared library
library 'jenkins-rnd-pipeline'

// Job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-patch-sh-centos7-01"
String testVmFqdn = "cd-vm-test-patch-sh-centos7-01.intern.epages.de"

String envOs = "centos"
String envType = "patch"
String envIdentifier = "singlehost"
String configuration = "~/epages-infra/configuration/install-epages/simple/install-epages-BRUS.conf"
String excludedFiles = "'mysql_config.txt DBSchema/GlobalVariables DBSchema/Columns DBSchema/KeyColumnUsage DBSchema/NonUniqueIndexes DBSchema/Tables XML/DemoShop XML/ShopTypes'"

String snapshotName = previousVersion

/**
 * vSphere Plugin:
 *
 * 1. Revert to Snapshot.
 * 2. Power-on/Resume VM.
 */
node(vSphereVm) {
  vCenter.revertToSnapshot(testVmFqdn, snapshotName)
  vCenter.powerOnVm(testVmFqdn)
}

/**
 * Patch - Singlehost - CentOS 7:
 *
 * 1. Run puppet agent test.
 * 2. Patch.
 * 3. Install additional Cartridges.
 * 4. Create Fingerprint.
 * 5. Copy Fingerprint.
 * 6. Compare language files.
 * 7. Check language tags.
 * 8. Check duplicated language tags.
 * 9. Check object references.
 */
node(testVm) {
  configure.setTime(envOs)
  puppetAgent.test()
  patch.patch(version)
  install.installAdditionalCartridges(configuration)
  fingerprint.createFingerprint(excludedFiles)
  fingerprint.copyFingerprint(version, repo, envIdentifier, envType)
  test.compareLanguageFiles()
  test.checkLanguageTags("Store")
  test.checkDuplicatedLanguageTags()
  test.checkObjectReferenceTest()
}

/**
 * vSphere Plugin:
 *
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVmFqdn);
}
