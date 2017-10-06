#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")

// VMs
String vSphereVm = "always_online"
String testVm = "cd-vm-test-install-005.intern.epages.de"
String testVmName = "cd-vm-test-install-005"

String snapshotName = "OS_with_puppet"
String installConf = "simple/install-epages-i18n.conf"
String envType = "install"
String envIdentifier = "singlehost-am"
String envOs = "centos"

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
 * Install - AM - Debian 8:
 * 1. Run Puppet Agent Test.
 * 2. Update Mirrors.
 * 3. Install.
 * 4. Create Fingerprint.
 * 5. Copy Fingerprint.
 */
node(testVmName) {
  configure.setTime(envOs)
  puppetAgent.test()
  configure.updateMirrors()
  install.installEpagesFromVersion(installConf, version)
  fingerprint.createFingerprint()
  fingerprint.copyFingerprint(version, repo, envIdentifier, envType)
}

/**
 * vSphere Plugin:
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
  vCenter.powerOffVm(testVm);
}
