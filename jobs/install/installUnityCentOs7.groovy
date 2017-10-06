#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-install-unity"

String snapshotName = "ci_source"
String installConf = "simple/install-epages-unity.conf"
String envType = "install"
String envIdentifier = "unity"
String shop = "esf-test"
String envOs = "centos"

// ESF parameters.
esfParameter = [
  new StringParameterValue("GROUPS", "UNITY"),
  new StringParameterValue("SHOP", shop),
  new StringParameterValue("URL", "http://cd-vm-test-install-unity.vm-intern.epages.com/epages"),
  new StringParameterValue("SURL", "http://${shop}.cd-vm-test-install-unity.vm-intern.epages.com"),
  new StringParameterValue("REPO", repo),
  new StringParameterValue("VERSION", version),
  new StringParameterValue("ENV_OS", "centos7"),
  new StringParameterValue("ENV_TYPE", envType),
  new StringParameterValue("ENV_IDENTIFIER", envIdentifier)
]

String excludeFiles = "XML/ShopTypes"

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
 * Install - Singlehost - Debian 8:
 * 1. Deploy epages-infra.
 * 2. Update Mirrors.
 * 3. Install.
 * 4. Configure unity ssl.
 * 5. Create shops.
 * 6. Create Fingerprint.
 * 7. Copy Fingerprint.
 * 8. ESF Unity tests.
 */
node(testVm) {
  configure.setTime(envOs)
  epagesInfra.deployEpagesInfra()
  configure.updateMirrors()
  install.installEpagesFromVersion(installConf, version)
  configure.setupUnitySsl()
  configure.createUnityShops()
  fingerprint.createFingerprint(excludeFiles)
  fingerprint.copyFingerprint(version, repo, envIdentifier, envType)
  esf.runEsfUnityTests(esfParameter)
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
