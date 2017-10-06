#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String version = getProperty("VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-install-006"
String snapshotName = "OS_with_functional_puppet"

String installConf = "security/install-epages.conf"
String importPath = "/Providers/Distributor"
String importFilePath = "/root/epages-infra/configuration/nightlies/DeveloperShopType.xml"
String store = "Store"
String site = "Site"

esfParameters = [
  new StringParameterValue("TARGET_DOMAIN", "cd-vm-test-install-006.epages.systems"),
  new StringParameterValue("TESTGROUPS", "SSLPROXY"),
  new StringParameterValue("SHOP", "security-test"),
  new StringParameterValue("ENV_OS", "debian"),
  new StringParameterValue("ENV_TYPE", "install"),
  new StringParameterValue("ENV_IDENTIFIER", "security"),
  new StringParameterValue("SHOP_DOMAIN", "songoku.epages.systems"),
]

/**
 * vSphere Plugin:
 *
 * 1. Revert to Snapshot.
 * 2. Power-on/Resume VM.
 */
node(vSphereVm) {
  vCenter.revertToSnapshot(testVm, snapshotName)
  vCenter.powerOnVm(testVm)
}

/**
 * Install - Run ESF security tests:
 *
 * 1. Get actual epages-infra
 * 2. Install.
 * 3. Install ssl proxy..
 * 4. Run all scheduler on store.
 * 5. Import Developer shoptypes.
 * 6. Enable SSL for Store.
 * 7. Enable SSL for Site.
 * 8. Create shop for security test.
 * 9. Set SMTP server for Site.
 * 10. Set SMTP serverfor Store.
 * 11. Get prperties from file.
 * 12. Run ESF tests.
 */
node(testVm) {
  epagesInfra.deployEpagesInfra()
  install.installEpagesFromVersion(installConf, version)
  configure.installSslProxy()
  configure.runSchedulerOnStore("Monitor")
  configure.importPl(importPath, importFilePath)
  configure.enableSSL(store)
  configure.enableSSL(site)
  configure.createShop("security-test")
  configure.setPl(store, "/", "SMTPServer=localhost")
  configure.setPl(site, "/", "SMTPServer=localhost")
  esf.runEsfTests(esfParameters)
}

/**
 * vSphere Plugin:
 *
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
