#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-master-04"
String snapshotName = "OS"

String installConf = "simple/install-epages-390MBx6.conf"

// Test shop names
String testShopCORE = "esf-auat-CHROME-Core-" + new Date().getTime()
String testShopSEARCH = "esf-auat-CHROME-Search-" + new Date().getTime()
String testShopERP = "esf-auat-CHROME-ERPShop-" + new Date().getTime()
String testShopeCMS = "esf-auat-CHROME-eCMS-" + new Date().getTime()

def shopMap = [:]
shopMap.put(testShopCORE.toString(),'Demo')
shopMap.put(testShopSEARCH.toString(),'Demo')
shopMap.put(testShopERP.toString(),'ERPDemo')
shopMap.put(testShopeCMS.toString(),'eCMSFree')

def baseParameter = [
  new StringParameterValue("TARGET_DOMAIN", "cd-vm-test-master-04.intern.epages.de"),
  new StringParameterValue("ENV_OS", "centos"),
  new StringParameterValue("ENV_TYPE", "install"),
  new StringParameterValue("ENV_IDENTIFIER", "singlehost"),
  new StringParameterValue("SKIPPRECONDITIONS", "true"),
  new StringParameterValue("RESTART_BROWSER", "false")
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
 * Install - Run ESF test CORE, SEARCH, ERP and eCMS - Singlehost - Centos 6:
 *
 * 1. Get epages-infra.
 * 2. Install.
 * 3. Enable SSL for Site.
 * 4. Enable SSl for Store.
 * 5. Stop cron jobs.
 * 6. Create shops.
 * 7. Update search index.
 * 8. Run ESF tests.
 * 9. Archive all .log files.
 */
node(testVm) {
  epagesInfra.deployEpagesInfra()
  install.installEpagesFromVersion(installConf, version)
  configure.enableSSL("Site")
  configure.enableSSL("Store")
  configure.stopCronJobs()
  configure.createShops(shopMap)
  configure.updateSearchIndex()
  parallel (
    core: {
      def esfParameters = baseParameter + [
                          new StringParameterValue("SHOP", testShopCORE),
                          new StringParameterValue("TESTGROUPS", "CORE")]
      esf.runEsfTests(esfParameters)
    },
    search: {
      def esfParameters = baseParameter + [
                          new StringParameterValue("SHOP", testShopSEARCH),
                          new StringParameterValue("TESTGROUPS", "SEARCH")]
      esf.runEsfTests(esfParameters)
    },
    erp: {
      def esfParameters = baseParameter + [
                          new StringParameterValue("SHOP", testShopERP),
                          new StringParameterValue("TESTGROUPS", "ERPShop")]
      esf.runEsfTests(esfParameters)
    },
    eCMS: {
      def esfParameters = baseParameter + [
                          new StringParameterValue("SHOP", testShopCORE),
                          new StringParameterValue("TESTGROUPS", "eCMS")]
      esf.runEsfTests(esfParameters)
    },
  )
}

/**
 * vSphere Plugin:
 *
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
