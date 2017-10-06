#!/usr/bin/env groovy

// Shared library
library 'jenkins-rnd-pipeline'

// Job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

// VMs
String vSphereVm = "always_online"
String testVmAsjs = "dbranch-vm-patch-di-asjs-centos-01"
String testVmAsjsFqdn = "dbranch-vm-patch-di-asjs-centos-01.intern.epages.de"
String envIdentifierAsjs = "distributed_three_hosts-asjs"

String testVmDb = "dbranch-vm-patch-di-db-centos-01"
String testVmDbFqdn = "dbranch-vm-patch-di-db-centos-01.intern.epages.de"
String envIdentifierDb = "distributed_three_hosts-db"

String testVmWsrr = "dbranch-vm-patch-di-wsrr-centos-01"
String testVmWsrrFqdn = "dbranch-vm-patch-di-wsrr-centos-01.intern.epages.de"
String envIdentifierWsrr = "distributed_three_hosts-wsrr"

String snapshotName = previousVersion

// Fingerprint
String envType = "patch"
String envOs = "debian"
String envIdentifier = "distributed_three_hosts"
String configuration = "~/epages-infra/configuration/install-epages/simple/install-epages-distributed.conf"

// ESF parameters
esfParameter = [
  new StringParameterValue("TESTGROUPS", "SMOKETEST"),
  new StringParameterValue("SHOP", "DemoShop"),
  new StringParameterValue("TARGET_DOMAIN", testVmWsrrFqdn),
  new StringParameterValue("VERSION", version),
  new StringParameterValue("REPO", repo),
  new StringParameterValue("ENV_OS", envOs),
  new StringParameterValue("ENV_TYPE", envType),
  new StringParameterValue("ENV_IDENTIFIER", envIdentifier)]

/**
 * vSphere Plugin:
 *
 * 1. Revert to Snapshot.
 * 2. Power-on/Resume VM.
 * 3. Revert to Snapshot.
 * 4. Power-on/Resume VM.
 * 5. Revert to Snapshot.
 * 6. Power-on/Resume VM.
 */
node(vSphereVm) {
  vCenter.revertToSnapshot(testVmAsjs, snapshotName)
  vCenter.powerOnVm(testVmAsjs)
  vCenter.revertToSnapshot(testVmDb, snapshotName)
  vCenter.powerOnVm(testVmDb)
  vCenter.revertToSnapshot(testVmWsrr, snapshotName)
  vCenter.powerOnVm(testVmWsrr)
}

/**
 * Prepare patching on alpha VM.
 *
 * 1. Checkout epages-infra.
 * 2. Prepare patch distributed.
 */
node(testVmAsjs) {
  epagesInfra.deployEpagesInfra()
  patch.patch(version, true)
}

/**
 * Prepare patching on database VM.
 *
 * 1. Checkout epages-infra.
 * 2. Prepare patch distributed.
 */
node(testVmDb) {
  epagesInfra.deployEpagesInfra()
  patch.patch(version, true)
}

/**
 * Prepare patching on webserver/request router VM.
 *
 * 1. Checkout epages-infra.
 * 2. Prepare patch distributed.
 */
node(testVmWsrr) {
  epagesInfra.deployEpagesInfra()
  patch.patch(version, true)
}

/**
 * Update the host file/Patch "prepare" step on the alpha VM.
 *
 * 1. Update host file.
 * 2. Patch step "Prepare".
 */
node(testVmAsjs) {
  configure.updateHostFile(version, testVmAsjsFqdn, testVmDbFqdn, testVmWsrrFqdn)
  patch.callPatchStep(version, "prepare")
}

/**
 * Patch "preinstall" step on the database VM.
 *
 * 1. Patch step "preinstall".
 */
node(testVmDb) {
  patch.callPatchStep(version, "preinstall")
}

/**
 * Patch "preinstall" step on the webserver/request router VM.
 *
 * 1. Patch step "preinstall".
 */
node(testVmWsrr) {
  patch.callPatchStep(version, "preinstall")
}

/**
 * Patch "install" step on the alpha VM.
 *
 * 1. Patch step "install".
 */
node(testVmAsjs) {
  patch.callPatchStep(version, "install")

}

/**
 * Patch "install" step on the database VM.
 *
 * 1. Patch step "postinstall"/Finish patching.
 * 2. Create Fingerprint for database VM.
 * 3. Copy Fingerprint for database VM.
 */
node(testVmDb) {
  patch.callPatchStep(version, "postinstall")
  fingerprint.createFingerprint("Files/Config")
  fingerprint.copyFingerprint(version, repo, envIdentifierDb, envType)
}

/**
 * Patch "postinstall" step on the webserver/request router VM.
 *
 * 1. Patch step "postinstall"/Finish patching.
 * 2. Create Fingerprint for webserver/request router VM.
 * 3. Copy Fingerprint for weserver/request router VM.
 */
node(testVmWsrr) {
  patch.callPatchStep(version, "postinstall")
  fingerprint.createFingerprint("Files/Config")
  fingerprint.copyFingerprint(version, repo, envIdentifierWsrr, envType)

}

/**
 * Reopen alpha VM, Create fingerprint, Run ESF tests.
 *
 * 1. Patch step "reopen".
 * 2. Install additional cartridges.
 * 3. Create fingerprint for alpha VM.
 * 4. Copy fingerprint for alpha VM.
 * 5. Configure application server.
 * 6. Stop ePages cron jobs.
 * 7. Run ESF tests.
 */
node(testVmAsjs) {
  patch.callPatchStep(version, "reopen")
  install.installAdditionalCartridges(configuration)
  fingerprint.createFingerprintDistributedPatch("XML/ShopTypes")
  fingerprint.copyFingerprint(version, repo, envIdentifierAsjs, envType, "/tmp/epages-fingerprint")
  configure.configureAppServers("390" , "3")
  configure.stopCronJobs()
  esf.runEsfTests(esfParameter)
}


/**
 * vSphere Plugin:
 *
 * 1. Power-off/Resume VM.
 * 2. Power-off/Resume VM.
 * 3. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVmAsjs);
  vCenter.powerOffVm(testVmDb);
  vCenter.powerOffVm(testVmWsrr);
}
