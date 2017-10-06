#!/usr/bin/env groovy

/**
 * Checkout the project esf-epages6 from github.
 */
def checkout() {

  stage("Checkout ${env.BRANCH_NAME}") {
    checkout scm 
  }
}

/**
 * Build the project esf-epages6 with gradle in an docker container.
 */
def build(String branch) {

  stage("Build ${branch}") {
    try {
      sh "chmod +x gradlew"
      sh "docker run --rm -t -v ${pwd()}:/opt/build -v ${pwd()}/esf-epages6:/opt/esf-epages6 epages/oracle-jdk8-fontconfig-freetype:latest" +
        " /bin/bash -c 'cd /opt/build; ./gradlew clean dist -q;'"
      sh "docker build -t epages/esf:${branch} ."
    } finally {
      echo "Clean up workspace, Removing old esf packages"
      deleteDir()
    }
  }
}

/**
 * Build the project esf-epages6 with gradle in an docker container.
 */
def buildWithSonarRunner(String branch) {

  stage("Build ${branch}") {
    try {
      sh "chmod +x gradlew"
      sh "docker run --rm -t -v ${pwd()}:/opt/build -v ${pwd()}:esf-epages6:opt/esf-epages6 epages/oracle-jdk8-fontconfig-freetype:latest" +
        " /bin/bash -c 'cd /opt/build; ./gradlew clean dist sonarRunner -q;'"
      sh "docker build -t epages/esf:${branch} ."
    } finally {
      echo "Clean up workspace, Removing old esf packages"
      deleteDir()
    }
  }
}

/**
 * Publish the artifacts for esf-epages6 for other projects (esf-epages-projects, esf-epages-unity).
 */
def publishArtifacts() {

  stage("Publish to artifactory") {
    withCredentials([[
      $class: 'UsernamePasswordMultiBinding',
      credentialsId: '',
      passwordVariable: 'ARTIFACTORY_PASSWORD',
      usernameVariable: 'ARTIFACTORY_USER']]) {
        sh "docker run --rm -t -v ${pwd()}:/opt/build -v ${pwd()}/esf-epages6:/opt/esf-epages6 epages/oracle-jdk8-fontconfig-freetype:latest" + 
          " /bin/bash -c 'cd /opt/build; ./gradlew -PartifactUser=${env.ARTIFACTORY_USER} -PartifactPswd=${env.ARTIFACTORY_PASSWORD} publish'"
    }
  }
}

/**
 * Test all esf-epages6 test against unstable-main.
 */
def testAll(String branch) {
  
  stage("Test all") {
    try {
      echo "Run ESF tests"
      // parallel testing of all ESF epages6 tests, Maybe use Run ESF tests.
    } finally { }
  }
}

/**
 * What is paused here?
 */
def pauseDuringNightly() {

  stage("Pause during nightly installation") {
    sh "${HOME}/epages-infra/scripts/esf/pause-during-installation.sh"
  }
}

/**
 * Publish the finished esf-epages6 docker image to docker hub.
 */
def publish(String branch) {

  stage("Publish") {
    withCredentials([[
      $class: 'UsernamePasswordMultiBinding',
      credentialsId: '5d80ffaa-b0af-4369-9b79-1b35557587d1',
      passwordVariable: 'DOCKER_PASSWORD',
      usernameVariable: 'DOCKER_USER']]) {
        sh "docker tag epages/esf:${branch} epages/esf:latest"
        sh "docker login -u ${env.DOCKER_USER} -p ${env.DOCKER_PASSWORD}; docker push epages/esf:latest"
    }
  }
}

// Important for the Pipeline Remote Loader Plugin. Avoid null pointer exception.
return this