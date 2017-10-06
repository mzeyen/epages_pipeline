#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")

// VMs
String vSphereVm = "always_online"

String testVm = "dbranch-vm-install-ms-centos-01"
String testVmName = "dbranch-vm-install-ms-centos-01.intern.epages.de"

String snapshotName = "OS"
String installConf = "simple/install-multistores.conf"
String envOs = "centos"
String envType = "install"
String envIdentifier = "multistores"
String shop = "eRetail"
String store = "Master"
String cartridge = "DE_EPAGES::MultiStoreMasterConnector"

String excludeFiles = "'mysql_config.txt DBSchema/GlobalVariables XML/ShopTypes'"

// ESF parameters.
esfParameter = [
  new StringParameterValue("TARGET_DOMAIN", testVmName),
  new StringParameterValue("TESTGROUPS", "SEARCH,CORE"),
  new StringParameterValue("SHOP", shop),
  new StringParameterValue("STORE", store),
  new StringParameterValue("ENV_OS", envOs),
  new StringParameterValue("ENV_TYPE", envType),
  new StringParameterValue("ENV_IDENTIFIER", envIdentifier),
  new StringParameterValue("SKIPPRECONDITIONS", "true")
]

esfMultistoreParameter = [
  new StringParameterValue("TARGET_DOMAIN", testVmName),
  new StringParameterValue("TESTGROUPS", "MULTISTORE"),
  new StringParameterValue("SHOP", shop),
  new StringParameterValue("STORE", store),
  new StringParameterValue("ENV_OS", envOs),
  new StringParameterValue("ENV_TYPE", envType),
  new StringParameterValue("ENV_IDENTIFIER", envIdentifier),
  new StringParameterValue("SKIPPRECONDITIONS", "true")
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
 * Install - Multistores - CentOS 6:
 * 1. Run Puppet Agent Test.
 * 2. Update Mirrors.
 * 3. Install.
 * 4. Add cartridge to Business unit.
 * 5. Create Fingerprint.
 * 6. Copy Fingerprint.
 * 7. Configue App server.
 * 8. Stop cron jobs.
 * 9. Start ESF tests for groups CORE, SEARCH.
 * 10. Start ESF tests for group MULTISTORE.
 */
node(testVm) {
  configure.setTime(envOs)
  epagesInfra.deployEpagesInfra()
  configure.updateMirrors()
  install.installEpagesFromVersion(installConf, version)
  configure.addCartridgesToBu(store, cartridge)
  //configure.exportMastershopProducts(store)
  fingerprint.createFingerprint(excludeFiles)
  fingerprint.copyFingerprint(version, repo, envIdentifier, envType)
  configure.configureAppServers("360", "3")
  configure.stopCronJobs()
  esf.runEsfTests(esfParameter)
  // See ESF-1620
  //esf.runEsfTests(esfMultistoreParameter)
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
