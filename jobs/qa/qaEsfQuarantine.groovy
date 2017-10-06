#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'


// job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-install-006.epages.systems"
String testNodeVM = "cd-vm-test-install-006"
String snapshotName = "OS_with_functional_puppet"

String installConf = "security/install-epages.conf"
String importPath = "/Providers/Distributor"
String importFilePath = "/root/epages-infra/configuration/nightlies/DeveloperShopType.xml"
String store = "Store"
String site = "Site"

esfParametersSecurity = [
  new StringParameterValue("TARGET_DOMAIN", "cd-vm-test-install-006.epages.systems"),
  new StringParameterValue("TESTGROUPS", "SSLPROXY"),
  new StringParameterValue("SKIPPRECONDITIONS", "true"),
  new StringParameterValue("ENV_OS", "debian"),
  new StringParameterValue("ENV_TYPE", "install"),
  new StringParameterValue("ENV_IDENTIFIER", "security"),
  new StringParameterValue("SHOP_DOMAIN", "songoku.epages.systems")
]

esfParametersQuaratine = [
  new StringParameterValue("TARGET_DOMAIN", "cd-vm-test-install-006.epages.systems"),
  new StringParameterValue("TESTGROUPS","CORE,SEARCH,THIRDPARTY"),
  new StringParameterValue("RUN_QUARANTINE_TESTS", "true"),
  new StringParameterValue("RETRY_TESTS", "false"),
  new StringParameterValue("SKIPPRECONDITIONS", "true"),
  new StringParameterValue("REPO", repo),
  new StringParameterValue("VERSION", version),
  new StringParameterValue("ENV_OS", "debian"),
  new StringParameterValue("ENV_TYPE", "install"),
  new StringParameterValue("ENV_IDENTIFIER", "security")

]

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
 * Install - Run ESF security and quaratine tests:
 *
 * 1. Get actual epages-infra
 * 2. Install.
 * 3. Install ssl proxy..
 * 4. Run all scheduler on store.
 * 5. Import Developer shoptypes.
 * 6. Enable SSL for Store.
 * 7. Enable SSL for Site.
 * 8. Set SMTP server for Site.
 * 9. Set SMTP serverfor Store.
 * 10. Get prperties from file.
 * 11. Run ESF tests for security.
 * 12. Runs ESF tests for quaratine tests.
 */
node(testNodeVM) {
  epagesInfra.deployEpagesInfra()
  install.installEpagesFromVersion(installConf, version)
  configure.installSslProxy()
  configure.runSchedulerOnStore("Monitor")
  configure.importPl(importPath, importFilePath)
  configure.enableSSL(store)
  configure.enableSSL(site)
  configure.setPl(store, "/", "SMTPServer=localhost")
  configure.setPl(site, "/", "SMTPServer=localhost")
  esf.runEsfTests(esfParametersSecurity)
  esf.runEsfTests(esfParametersQuaratine)
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm)
}
