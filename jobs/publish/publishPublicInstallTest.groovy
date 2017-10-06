#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")
String snapshotName = "OS"

// VMs
String vSphereVm = "always_online"
String testVm = "to-install-debian8-02.epages.systems"

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
 * INSTALL - Singlehost - Debian 8:
 *
 * 1. Deploy epages-infra.
 * 2. Install previous version.
 * 3. Patch to published version.
 * 4. Create Fingerprint.
 * 5. Copy Fingerprint.
 */
node(testVm) {
  epagesInfra.deploy()
  install.installEpagesFromPublicRepo(previousVersion)
  patch.patchToPublishedVersion(version)
  fingerprint.createFingerprint(excludedFiles)
  fingerprint.copyFingerprint(version, repo, envIdentifier, envType)
}

/**
 * vSphere Plugin:
 *
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
