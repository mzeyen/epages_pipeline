#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String previousVersion = getProperty("PREVIOUS_VERSION")
String version = getProperty("VERSION")
String snapshotName = "OS"

// VMs
String vSphereVm = "always_online"
String testVm = "to-patch-centos7-01.epages.systems"

String envType = "patch"
String repo = "public"
String envIdentifier = "public"
String excludedFiles = "XML/ShopTypes"

def parameter = [
  new StringParameterValue("VERSION", version)
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
 * Patch - Singlehost - CentOS 7:
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
    stage("Patch pm.epages.com to new version $version") {
        build job: "publish/publishPatchPmVm", parameters: parameter
    }
}

/**
 * vSphere Plugin:
 *
 * 1. Power-off/Resume VM.
 */
node(vSphereVm) {
    vCenter.powerOffVm(testVm);
}
