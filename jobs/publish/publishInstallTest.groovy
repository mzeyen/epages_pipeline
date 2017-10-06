#!/usr/bin/env groovy

// Shared library
library 'jenkins-rnd-pipeline'

// Job parameter
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

String vSphereVm = "always_online"
String testVm = "to-install-centos7-01.epages.systems"
String snapshotName = "OS"
String installConf = "simple/install-epages.conf"


String envType = "install"
String repo = "public"
String envIdentifier = "public"

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
 * INSTALL - Singlehost - CentOs:
 *
 * 1. Deploy epages-infra.
 * 2. Install published version.
 * 4. Create Fingerprint.
 * 5. Copy Fingerprint.
 */
node(testVm) {
  epagesInfra.deploy()
  install.installEpagesFromVersion(installConf, version)
  fingerprint.createFingerprint()
  fingerprint.copyFingerprint(version, repo, envIdentifier, envType)
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm)
}
