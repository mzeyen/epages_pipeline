#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String repo = getProperty("REPO")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-master-50.epages.systems"
String snapshotName = "OS_Java"

String installConf = "simple/install-epages-390MBx3.conf"

// Test shop names
String testShopTHIRDPARTY = "esf-auat-cdp-esf-test-Thirdparty-" + new Date().getTime()
String testShopUS = "esf-auat-cdp-esf-test-US-" + new Date().getTime()
String testShopUSTAX = "esf-auat-cdp-esf-test-USTAX-" + new Date().getTime()

def shopMap = [:]
shopMap.put(testShopTHIRDPARTY.toString(),'Demo')
shopMap.put(testShopUS.toString(),'Demo')
shopMap.put(testShopUSTAX.toString(),'Demo')

def cartridge_list = ["KlarnaFinancing",
                      "KlarnaHirePurchase",
                      "KlarnaInvoice",
                      "PayPalExpress",
                      "PayPalIntegralEvolution",
                      "Ogone",
                      "Saferpay"]

String dockerTag = "epages/esf:latest-thirdparty"

def baseParameter = [
  new StringParameterValue("TARGET_DOMAIN", "cd-vm-test-master-50.epages.systems"),
  new StringParameterValue("ENV_OS", "centos"),
  new StringParameterValue("ENV_TYPE", "install"),
  new StringParameterValue("ENV_IDENTIFIER", "singlehost"),
  new StringParameterValue("SKIPPRECONDITIONS", "true"),
  new StringParameterValue("RESTART_BROWSER", "false")
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
 * Install - Run ESF test THIRDPARTY and US:
 *
 * 1. Copy artefact from job 'cdp_Tag_MAIN'.
 * 2. Run puppet agent test.
 * 3. Install.
 * 4. Enable SSL for Site.
 * 5. Enable SSl for Store.
 * 6. Stop cron jobs.
 * 7. Start HTTPD service.
 * 8. Copy SSL certificates and keys.
 * 9. Create shops for each test group in THIRDPARTY and US.
 * 10. Create shop with DemoUS shoptype.
 * 11. Update search index.
 * 12. Activate logging for all klarna payments.
 * 13. Prepare store "Store" for  TaxUS tests.
 * 14. Get properties from file.
 * 15. Run ESf job "Run_ESF_tests".
 * 16. Archive artefacts.
 */
node(testVm) {
  epagesInfra.deployEpagesInfra()
  install.installEpagesFromRepo(installConf, repo)
  configure.enableSSL("Site")
  configure.enableSSL("Store")
  configure.stopCronJobs()
  configure.getSslCertificates()
  configure.createShops(shopMap)
  configure.updateSearchIndex()
  configure.activateLogging(cartridge_list)
  configure.prepareTaxUsTest()
  parallel (
    thirdparty: {
      def esfParameters = baseParameter + [
                          new StringParameterValue("SHOP", testShopTHIRDPARTY),
                          new StringParameterValue("DOCKER_TAG", dockerTag),
                          new StringParameterValue("TESTGROUPS", "THIRDPARTY")]
      esf.runEsfTests(esfParameters)
    },
    us: {
      def esfParameters = baseParameter + [
                          new StringParameterValue("SHOP", testShopUS),
                          new StringParameterValue("DOCKER_TAG", dockerTag),
                          new StringParameterValue("TESTGROUPS", "US")]
      esf.runEsfTests(esfParameters)
    },
    ustax: {
      def esfParameters = baseParameter + [
                          new StringParameterValue("SHOP", testShopUSTAX),
                          new StringParameterValue("DOCKER_TAG", dockerTag),
                          new StringParameterValue("TESTGROUPS", "USTAX")]
      esf.runEsfTests(esfParameters)
    },
  )
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
