#!/usr/bin/env groovy

String installVM = getProperty("installVM")
String branch = getProperty("branch")
String repository = getProperty("repository")
String remote = getProperty("remote")

/**
 * Install Rest-Test-Gold from a specific branch.
 *
 * 1. Clean the workspace
 * 2. Checkout and load pipeline modules
 * 3. Checkout rest-test-gold repository
 * 4. Build the necessary docker container
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
    sh 'if [[ ! -z $(docker ps -a | grep rest-test-gold | awk \'{print $1}\') ]] ; then docker stop $(docker ps -a | grep rest-test-gold | awk \'{print $1}\') ; fi'
    sh 'rm -rf *'
    // Checkout the sources for jenkins-rnd-pipeline
    stage "Checkout pipeline scripts"
      checkout scm
      // Show parameters and load necessary scripts
      git = load("jobs/modules/git.groovy")

    // Checkout repository from specific branch
    stage "Checkout rest-test-gold"
      git.cloneFromBranch("${remote}", "${repository}", "${branch}")

    // We want to build the docker containers in the first step
    stage "Build docker container for RTG"
      sh "cd ${repository} && docker-compose stop"
      sh "cd ${repository} && docker-compose rm -f"
      sh "cd ${repository} && docker-compose build"
      sh "cd ${repository} && docker-compose up -d"

  } catch (exception) {
    throw exception
  }
}
