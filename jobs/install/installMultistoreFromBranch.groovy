#!/usr/bin/env groovy

// Get the necessary variables
String installVM = getProperty("installVM")
String branch = getProperty("branch")
String repository = getProperty("repository")
String remote = getProperty("remote")

/**
 * Install MultiStore from a specific branch.
 *
 * 1. Clean the workspace.
 * 2. Checkout and load pipeline modules.
 * 3. Checkout Cartridges.
 * 4. Copy Cartridges to the correct folder and start reinstall-
 * 5. Also install additional cartridges.
 *
 * @params installVM The VM, where the installation should run.
 * @params branch The branch, from which the sources should come.
 * @params repository The repository, from which the sources should come.
 * @params remote The remote, from which the sources should come.
 */

node("${installVM}") {
  try {
    // Clean workspace before starting
    stage "Clean workspace"
      sh 'rm -rf *'

    // Checkout the sources for jenkins-rnd-pipeline
    stage "Checkout pipeline scripts"
      checkout scm
      // Show parameters and load necessary scripts
      git = load("jobs/modules/git.groovy")
      install = load("jobs/modules/install.groovy")

    // Checkout repository from specific branch
    stage "Checkout Cartridges"
      git.cloneFromBranch("${remote}", "${repository}", "${branch}")

    // Make a reinstall with given sources
    stage "Make reinstall with multistore and install additional cartridges"
      // Copy the sources to the cartridges directory
      sh 'source /etc/default/epages6 &&' +
            'rm -rf ${EPAGES_CARTRIDGES} &&' +
            'cp -r ' + repository + ' ${EPAGES_CARTRIDGES}'

      // Start reinstall for multistore
      install.reinstallEpagesMS()

    // Install additional cartridges for multistore
      install.installAdditionalCartridgesMS()

  } catch (exception) {
    throw exception
  }
}