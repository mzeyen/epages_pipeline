#!/usr/bin/env groovy

// VMs
String testVM = "cd-vm-test-master-08"

// Modules
def configure = load '../modules/configure.groovy'

/**
 * Run EUP tests - Singlehost - CentOS 6:
 * 1. Login to docker
 * 2. Pull docker containers.
 * 3. Get properties from file.
 * 4. Run tests inside docker container.
 * 5. Save log files.
 */
 node(${testVM}) {
  configure.createShop()
  configure.getProperties()
  configure.updateSearchIndex()
}

/**
 * ToDo:
 * Build other project "Init_eup_shop_with_ESF".
 * Get parameters from esf.properties
 */

/**
 * ToDo:
 * Trigger when build is stable: "cdp_Run_eup_tests".
 * Get parameters from esf.properties
 */
