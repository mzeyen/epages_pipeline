#!/usr/bin/env groovy

/**
 * Login into docker.
 */
def login(String credentialsId) {

  stage('Login into docker.') {
	  withCredentials([[
	    $class: 'UsernamePasswordMultiBinding',
	    credentialsId: '${credentialsId}',
	    passwordVariable: 'PASSWORD',
	    usernameVariable: 'USER']]) {
	  	sh "docker login -u ${env.USER} -p ${env.PASSWORD}"
		}
  }
}

/**
 * Pull from docker repo.
 */
def pull() {

  stage('Pull from docker repo.') { }
}

/**
 * Run tests inside docker container.
 */
def runTests() {

  stage('Run tests inside docker container.') { }
}
