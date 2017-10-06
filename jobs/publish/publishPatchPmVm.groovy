#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String version = getProperty("VERSION")

// VMs
String vSphereVm = "pm.epages.com"

/**
 * 1. Deploy epages-infra.
 * 2. Remove old Patch folders.
 * 3. Patch to published version.
 */
node(vSphereVm) {
  epagesInfra.deploy()
  patch.removePatchesFolder()
  patch.patchToPublishedVersion(version)
}
