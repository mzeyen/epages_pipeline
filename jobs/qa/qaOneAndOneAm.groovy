#!/usr/bin/env groovy

library 'jenkins-rnd-pipeline'

// VMs
String vSphereVm = "always_online"
String testVm = "to-qa-1und1-debian8"
String repo = getProperty("REPO")
String dockerVm = "esf_docker"

// Test shop names
String testShopUS = "us-tests-" + new Date().getTime()
String testShopUSTAX = "ustax-tests-" + new Date().getTime()
String testShopERP = "erp-tests-" + new Date().getTime()
String testShopeCMS = "esf-auat-CHROME-eCMS-" + new Date().getTime()

def shopMap = [:]
shopMap.put(testShopUS.toString(),'Demo')
shopMap.put(testShopUSTAX.toString(),'Demo')
shopMap.put(testShopERP.toString(),'ERPDemo')
shopMap.put(testShopeCMS.toString(),'eCMSFree')

String installConf="simple/install-epages-490MBx8.conf"

// ESF parameters.
def baseParameter = [
  new StringParameterValue("TARGET_DOMAIN", "to-qa-1und1-debian8.epages.systems"),
  new StringParameterValue("ENV_OS", "debian"),
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
  vCenter.revertToSnapshot(testVm, "OS")
  vCenter.powerOnVm(testVm)
}

/**
 * Install - Normal - Singlehost - Debian 8:
 * 1. Set corret time on VM.
 * 2. Clone infra repo.
 * 3. Install release.
 * 4. Set smtp server for Site.
 * 5. Set smtp server for Store.
 * 6. Apply security changes.
 * 7. Get SSl certificates.
 * 8. Create shops for esf tests.
 */
 node(testVm) {
  epagesInfra.deployEpagesInfra()
  install.installEpagesFromRepo(installConf,repo)
  configure.setPl("Site", "/","SMTPServer=127.0.0.1")
  configure.setPl("Store", "/","SMTPServer=127.0.0.1")
  configure.applySecurityChanges()
  configure.getSslCertificates()
  configure.createShops(shopMap)
  parallel (
    core_customer_category: {
      def esfParameters = baseParameter + [
                          new StringParameterValue("TESTGROUPS", "CORE_CUSTOMER,CORE_CATEGORY,CORE_TAX")]
      esf.runEsfTests(esfParameters)
    },
    core_order_search: {
      def esfParameters = baseParameter + [
                          new StringParameterValue("TESTGROUPS", "CORE_ORDER,SEARCH")]
      esf.runEsfTests(esfParameters)
    },
    core_product: {
      def esfParameters = baseParameter + [
                          new StringParameterValue("TESTGROUPS", "CORE_PRODUCT")]
      esf.runEsfTests(esfParameters)
    },
    core_payment_style_bbo_forum_presentation_amazon: {
      def esfParameters = baseParameter + [
                          new StringParameterValue("TESTGROUPS", "CORE_PAYMENT,CORE_STYLE,CORE_BBO,CORE_FORUM,CORE_PRESENTATION,CORE_AMAZON")]
      esf.runEsfTests(esfParameters)
    },
    core_shop_coupon_guestbook: {
      def esfParameters = baseParameter + [
                          new StringParameterValue("TESTGROUPS", "CORE_SHOP,CORE_COUPON,CORE_GUESTBOOK")]
      esf.runEsfTests(esfParameters)
    },
    erpShop: {
      def esfParameters = baseParameter + [
                          new StringParameterValue("SHOP", testShopERP),
                          new StringParameterValue("TESTGROUPS", "ERPShop")]
      esf.runEsfTests(esfParameters)
    },
    ecms: {
      def esfParameters = baseParameter + [
                          new StringParameterValue("SHOP", testShopeCMS),
                          new StringParameterValue("TESTGROUPS", "eCMS")]
      esf.runEsfTests(esfParameters)
    },
    us: {
      def esfParameters = baseParameter + [
                          new StringParameterValue("SHOP", testShopUS),
                          new StringParameterValue("TESTGROUPS", "US")]
      esf.runEsfTests(esfParameters)
    },
    ustax: {
      def esfParameters = baseParameter + [
                          new StringParameterValue("SHOP", testShopUSTAX),
                          new StringParameterValue("TESTGROUPS", "USTAX")]
      esf.runEsfTests(esfParameters)
    }
  )
}

/**
 * vSphere Plugin:
 * 1. Power-off VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
