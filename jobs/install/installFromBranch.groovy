#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

String repository = getProperty("Repository")
String remote = getProperty("Remote")

// VMs
String nameVm = getProperty("VM")
String branch = getProperty("Branch")
String vSphereVm = "always_online"
String snapshotName = getProperty("Snapshot")

/**
 * vSphere Plugin:
 * 1. Revert to Snapshot.
 * 2. Power-on/Resume VM.
 */
node(vSphereVm) {
    vCenter.revertToSnapshot(nameVm, snapshotName)
    vCenter.powerOnVm(nameVm)
}

/**
 * Reinstall specific branch:
 * 1. Update epages-infra
 * 2. Install latest epages version from variables file
 * 3. Backup Cartridges folder.
 * 6. Checkout specified branch.
 * 7. Make reinstall.
 */
node(nameVm) {
    epagesInfra.deployEpagesInfra()
    install.installLatestEpagesFromVariables()
    configure.backupCartridgesFolder()
    gitActions.cloneCartridgesFromBranch(remote, repository, branch)
    install.reinstallEpages()
}
