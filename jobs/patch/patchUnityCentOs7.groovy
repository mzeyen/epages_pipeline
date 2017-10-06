#!/usr/bin/env groovy

// Shared library
library 'jenkins-rnd-pipeline'

// Job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-patch-unity"

String snapshotName = previousVersion
String configuration = "~/epages-infra/configuration/install-epages/simple/install-epages-unity.conf"
String envOs = "centos"
String envType = "patch"
String envIdentifier = "unity"
String shop = "esf-test"

// ESF parameters.
esfParameter = [
  new StringParameterValue("GROUPS", "UNITY"),
  new StringParameterValue("SHOP", shop),
  new StringParameterValue("URL", "http://cd-vm-test-patch-unity.vm-intern.epages.com/epages"),
  new StringParameterValue("SURL", "http://${shop}.cd-vm-test-patch-unity.vm-intern.epages.com"),
  new StringParameterValue("REPO", repo),
  new StringParameterValue("VERSION", version),
  new StringParameterValue("ENV_OS", envOs),
  new StringParameterValue("ENV_TYPE", envType),
  new StringParameterValue("ENV_IDENTIFIER", envIdentifier)
]

// Fingerprint
String excludeFiles = "XML/ShopTypes"

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
 * Patch - Singlehost - Unity - CentOS 7
 *
 * 1. Deploy epages-infra.
 * 2. Patch epages to the next version.
 * 3. Install additional cartridges
 * 4. Create Fingerprint.
 * 5. Copy Fingerprint.
 * 6. Start ESF unity tests.
 */
node(testVm) {
  epagesInfra.deployEpagesInfra()
  patch.patch(version)
  install.installAdditionalCartridges(configuration)
  fingerprint.createFingerprint(excludeFiles)
  fingerprint.copyFingerprint(version, repo, envIdentifier, envType)
  esf.runEsfUnityTests(esfParameter)
}

/**
 * vSphere Plugin:
 *
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
