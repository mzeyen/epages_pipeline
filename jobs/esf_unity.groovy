#!/usr/bin/env groovy

/**
 * Checkout the project esf-epages-unity from github.
 */
def checkout() {

  stage("Checkout ${env.BRANCH_NAME}") {
    checkout scm 
  }
}

/**
 * Build the project esf-epages-unity with gradle in an docker container.
 */
def build(String branch) {

  stage("Build ${branch}") {
    try {
      sh "chmod +x gradlew"
      sh "docker run --rm -t -v ${pwd()}:/opt/esf-epages-unity epages/oracle-jdk8-fontconfig-freetype:latest" +
        " /bin/bash -c 'cd /opt/esf-epages-unity; ./gradlew clean dist -q;'"
      sh "docker build -t epages/esf-epages-unity:${branch} ."
    } finally {
      echo "Clean up workspace, Removing old esf packages"
      deleteDir()
    }
  }
}

/**
 * Test all esf-epages-unity test against unstable-main.
 */
def testAll(String branch) {

  stage("Test all") {
    try {
      echo "Run ESF tests"
      def shopName = "pipeline-dev"
      sh "docker run --rm --user=root -v ${pwd()}/log:/home/esf/esf/log -t epages/esf-epages-unity:${branch}" +
        " -sub esf-epages-unity -groups UNITY" +
        " -email p.domin@epages.com -shop ${shopName}" +
        " -url https://greenteam-vm-01.intern.epages.de/epages" +
        " -surl https://${shopName}.greenteam-vm-01.intern.epages.de -ap 0 -sp -l de_DE -r;"
    } finally {
      echo "Archieving artifacts"
      archiveArtifacts artifacts: 'log/**', excludes: null
      echo "Clean up workspace, Removing old logs"
      deleteDir()
    }
  }
}

/**
 * Test all changed esf-epages-unity test against unstable-main.
 */
def testChanged(String branch) {

  stage("Test changes") {
    echo "This one is a pull request test."
  }
}

/**
 * Publish the finished esf-epages-unity docker image to docker hub.
 */
def publish(String branch) {

  stage("Publish") {
    withCredentials([[
      $class: 'UsernamePasswordMultiBinding',
      credentialsId: '5d80ffaa-b0af-4369-9b79-1b35557587d1',
      passwordVariable: 'PASSWORD',
      usernameVariable: 'USER']]) {
        sh "docker tag epages/esf-epages-unity:${branch} epages/esf-epages-unity:latest"
        sh "docker login -u ${env.USER} -p ${env.PASSWORD};" +
          "docker push epages/esf-epages-unity:latest"
    }
  }
}

/**
 * Merge something e.g.: from 'dev' to 'master' or 'master' to 'stable'.
 */
def merge(String originBranch, String targetBranch, String credentials) {

  stage("Merge ${originBranch} into ${targetBranch}") {
    echo "We want to merge from ${originBranch} to ${targetBranch}"
  }
}

// Important for the Pipeline Remote Loader Plugin. Avoid null pointer exception.
return this
