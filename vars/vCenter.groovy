#!/usr/bin/env groovy

/**
 * Create a new snapshot.
 *
 * @param vmName The name for the VM.
 * @param snapshotName The name for the snapshot to be created.
 * @param description (default: '').
 * @param includeMemory (default: false).
 * @param serverName (default: 'vcenter').
 */
def createSnapshot(String vmName, 
                   String snapshotName,
                   String description = '',
                   boolean includeMemory = false,
                   String serverName = 'vCenter') {

  stage("Create snapshot: ${snapshotName} on ${vmName}") {
    vSphere buildStep: [$class: 'TakeSnapshot', 
                        description: description,
                        includeMemory: includeMemory,
                        snapshotName: snapshotName,
                        vm: vmName],
                        serverName: serverName
  }
}

/**
 * Delete previous snapshot.
 *
 * @param vmName The name for the VM.
 * @param snapshotName The name for the snapshot to be deleted.
 * @param serverName (default: 'vcenter').
 */
def deleteSnapshot(String vmName, 
                   String snapshotName,
                   String serverName = 'vCenter') {

  stage("Delete snapshot: ${snapshotName} on ${vmName}") {
    vSphere buildStep: [$class: 'DeleteSnapshot',
                        consolidate: false,
                        failOnNoExist: false,
                        snapshotName: snapshotName,
                        vm: vmName],
                        serverName: serverName
  }
}

/**
 * Revert a VM to a given snapshot.
 *
 * @param vmName The name for the VM.
 * @param snapshotName The name of the snapshot.
 * @param serverName (default: 'vcenter').
 */
def revertToSnapshot(String vmName, 
                     String snapshotName, 
                     String serverName = 'vCenter') {

  stage("Revert snapshot to ${snapshotName} on ${vmName}") {
    vSphere buildStep: [$class: 'RevertToSnapshot',
                        snapshotName: snapshotName,
                        vm: vmName],
                        serverName: serverName
  }
}

/**
 * Power on a VM.
 *
 * @param vmName The name for the VM to be powered on.
 * @param timeoutInSeconds (default: 300).
 * @param serverName (default: 'vcenter').
 */
def powerOnVm(String vmName, 
              int timeoutInSeconds = 300,
              serverName = 'vCenter') {

  stage("Power On VM: ${vmName}") {
    vSphere buildStep: [$class: 'PowerOn', 
                        timeoutInSeconds: timeoutInSeconds,
                        vm: vmName],
                        serverName: serverName
  }
}

/**
 * Power off a VM.
 *
 * @param vmName The name for the VM to be powered off.
 * @param serverName (default: 'vcenter').
 */
def powerOffVm(String vmName,
               String serverName = 'vCenter') {

  stage("Power Off VM: ${vmName}") {
    vSphere buildStep: [$class: 'PowerOff', 
                        evenIfSuspended: false,
                        shutdownGracefully: true,
                        vm: vmName],
                        serverName: serverName
  }
}
