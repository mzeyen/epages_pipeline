#!/usr/bin/env groovy

// shared library
library 'jenkins-rnd-pipeline'

// job parameter
String repo = getProperty("REPO")
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

// VMs
String vSphereVM = "always_online"
String testVM = ""

String snapshotName = previousVersion


/**
 * ToDo.:
 */
