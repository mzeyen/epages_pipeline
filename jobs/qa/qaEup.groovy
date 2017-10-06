#!/usr/bin/env groovy

// VMs
String vSphereVM = "always_online"
String testVM = "cd-vm-docker-host-001"

// Credentials (Only use jenkins credential ids, if possible)
def DOCKER_USERNAME = ""
def DOCKER_PASSWORD = ""

// Modules
def vSphere = load '../modules/vSphere.groovy'
def artefacts = load '../modules/artefacts.groovy'
def docker = load '../modules/docker.groovy'
def configure = load '../modules/configure.groovy'

/**
 * Run EUP tests:
 * 1. Login to docker
 * 2. Pull docker containers.
 * 3. Get properties from file.
 * 4. Run tests inside docker container.
 * 5. Save log files.
 */
node(testVM) {
  docker.login()
  docker.pull()
  configure.createDir()
  configure.getProperties()
  docker.runTests()
  artefacts.archiveFiles()
}
