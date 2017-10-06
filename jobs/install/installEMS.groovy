#!/usr/bin/env groovy

// Get necessary variables
String installVM = getProperty("installVM")
String branch = getProperty("branch")
String repository = getProperty("repository")
String remote = getProperty("remote")

/**
 * Install EMS from a specific branch.
 *
 * 1. Clean workspace.
 * 2. Checkout pipeline scripts and load git groovy.
 * 3. Checkout multistore-MS.
 * 4. Build EMS docker container.
 * 5. Import demo data to the created EMS.
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
      // If container were found, remove them. Else go on.
      sh 'if [[ ! -z $(docker ps -a | grep multistore | awk \'{print $1}\') ]] ; then docker stop $(docker ps -a | grep multistore | awk \'{print $1}\') ; fi'
      sh 'rm -rf *'

    // Checkout the sources for jenkins-rnd-pipeline
    stage "Checkout pipeline scripts"
      // As the context is jenkins-rnd-pipeline here, this command will check out jenkins-rnd-pipeline for us.
      checkout scm
      // Show parameters and load necessary scripts
      def git = load("jobs/modules/git.groovy")

    // Checkout repository from specific branch
    stage "Checkout multistore-MS"
      // Use git module to checkout repository, as the context is in jenkins-rnd-pipeline, not multistore-MS repo.
      git.cloneFromBranch("${remote}", "${repository}", "${branch}")

    // We want to build the docker containers in the first step
    stage "Build docker container for EMS"
      sh "cd ${repository}/src && docker-compose stop"
      sh "cd ${repository}/src && docker-compose rm -f"
      sh "cd ${repository}/src && docker-compose build"
      sh "cd ${repository}/src && docker-compose up -d"

    // Create database and fill it with demo data
    stage "Import demo data to EMS"
      sh 'docker exec multistore-as-production-01 /bin/bash -c "RAILS_ENV=production rake db:create"'
      sh 'docker exec multistore-as-production-01 /bin/bash -c "RAILS_ENV=production rake db:schema:load"'
      sh 'docker exec multistore-as-production-01 /bin/bash -c "RAILS_ENV=production rake db:seed"'

  } catch (exception) {
    throw exception
  }
}