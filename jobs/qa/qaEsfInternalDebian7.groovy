#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-master-09"
String snapshotName = "puppet"

String installConf = "simple/install-epages-390MBx6.conf"

// Test shop names
String testShopCORE = "esf-auat-CHROME-Core-" + new Date().getTime()
String testShopSEARCH = "esf-auat-CHROME-Search-" + new Date().getTime()

def shopMap = [:]
shopMap.put(testShopCORE.toString(),'Demo')
shopMap.put(testShopSEARCH.toString(),'Demo')

def baseParameter = [
  new StringParameterValue("TARGET_DOMAIN", "cd-vm-test-master-09.vm-intern.epages.com"),
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
 * 6. Set SMTPServer to localhost on Store.
 * 7. Set SMTPServer to localhost on Site.
 * 8. Create shops for each test group in CORE and Search.
 * 9. Update search index.
 * 10. Run ESF tests.
 * 11. Archive artefacts.
 */
node(testVm) {
  epagesInfra.deployEpagesInfra()
  install.installEpagesFromRepo(installConf, repo)
  configure.enableSSL("Site")
  configure.enableSSL("Store")
  configure.stopCronJobs()
  configure.setPl("Store", "/", "SMTPServer=localhost")
  configure.setPl("Site", "/", "SMTPServer=localhost")
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
