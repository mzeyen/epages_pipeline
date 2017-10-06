#!/usr/bin/env groovy

import hudson.model.StringParameterValue
import org.jvnet.jenkins.plugins.nodelabelparameter.LabelParameterValue

/**
 * Checkout the project esfEpages from github.
 */
def checkout() {

  stage("Checkout ${env.BRANCH_NAME}") {
    checkout scm
  }
}

/**
 * Build the project esfEpages with gradle in an docker container.
 *
 * @param branch we want to build the esf for.
 */
def build(String branch) {

  stage("Build ${branch}") {
    try {
      sh """
        set -e
        chmod +x gradlew
        docker run --rm -t -v ${pwd()}:/opt/build -v ${pwd()}/esf-epages6:/opt/esf-epages6 \
        epages/oracle-jdk8-fontconfig-freetype:latest \
        /bin/bash -c 'cd /opt/build; ./gradlew clean dist sonarRunner esfCoverageReport;'
        docker build -t epages/esf:${branch} .
      """
    } finally {
      echo "Clean up workspace, Removing old esf packages"
      deleteDir()
    }
  }
}

/**
 * Build the project esfEpages with gradle in an docker container.
 *
 * @param oldTag tag we want to change.
 * @param newTag new image tag.
 */
def tagDockerImage(String oldTag, String newTag) {

  stage("Tag docker image ${oldTag} -> ${newTag}") {
    sh "docker tag epages/esf:${oldTag} epages/esf:${newTag}"
  }
}

/**
 * Publish the artifacts for esf.
 */
def publishArtifactory(String artifactoryCredentials) {

  stage("Publish artifactory") {
    withCredentials([[
      $class: 'UsernamePasswordMultiBinding',
      credentialsId: artifactoryCredentials,
      passwordVariable: 'ARTIFACTORY_PASSWORD',
      usernameVariable: 'ARTIFACTORY_USER']]) {
        sh """
          chmod +x gradlew
          docker run --rm -t -v ${pwd()}:/opt/build -v ${pwd()}/esf-epages6:/opt/esf-epages6 epages/oracle-jdk8-fontconfig-freetype:latest \
          /bin/bash -c 'cd /opt/build; ./gradlew -PartifactUser=${env.ARTIFACTORY_USER} -PartifactPswd=${env.ARTIFACTORY_PASSWORD} publish'
        """
      }
  }
}

/**
 * Test all esfEpages core test paralell against unstable-main.
 *
 * e.g.: 'SEARCH','CORE','US','USTAX','ERPSHOP','ECMS'
 *
 * @param branch The branch/docker tag we want to test
 */
def testAll(String branch) {

  def baseParameter = [
    new StringParameterValue("TARGET_DOMAIN", "schwarzbart.epages.systems"),
    new StringParameterValue("VERSION", "nightly"),
    new StringParameterValue("DOCKER_TAG", "epages/esf:" + branch),
    new StringParameterValue("ENV_OS", "centos"),
    new StringParameterValue("ENV_TYPE", "install"),
    new StringParameterValue("ENV_IDENTIFIER", "singlehost"),
    new StringParameterValue("REPO", "nightly/" + new Date().format('yyyyMMdd')),
    new StringParameterValue("SKIPPRECONDITIONS", "true"),
    new StringParameterValue("RESTART_BROWSER", "false")
  ]

  parallel (
    "CORE_CUSTOMER_CATEGORY_TAX" : {
      def esfParameters = baseParameter + [new StringParameterValue("TESTGROUPS", "CORE_CUSTOMER,CORE_CATEGORY,CORE_TAX")]
      esf.runEsfTests(esfParameters)
    },
    "CORE_ORDER": {
      def esfParameters = baseParameter + [new StringParameterValue("TESTGROUPS", "CORE_ORDER")]
      esf.runEsfTests(esfParameters)
    },
    "CORE_PRODUCT": {
      def esfParameters = baseParameter + [new StringParameterValue("TESTGROUPS", "CORE_PRODUCT")]
      esf.runEsfTests(esfParameters)
    },
    "CORE_PAYMENT": {
      def esfParameters = baseParameter + [new StringParameterValue("TESTGROUPS", "CORE_PAYMENT")]
      esf.runEsfTests(esfParameters)
    },
    "CORE_SHOP_COUPON_GUEST": {

      def esfParameters = baseParameter + [new StringParameterValue("TESTGROUPS", "CORE_SHOP,CORE_COUPON,CORE_GUESTBOOK")]
      esf.runEsfTests(esfParameters)
    },
    "CORE_STYLE_BBO_FORUM_PRES_AMAZON": {

      def esfParameters = baseParameter + [
                            new StringParameterValue("TESTGROUPS", "CORE_STYLE,CORE_BBO,CORE_FORUM,CORE_PRESENTATION,CORE_AMAZON")]
      esf.runEsfTests(esfParameters)
    },
    "SEARCH" : {
      def esfParameters = baseParameter + [
                            new StringParameterValue("SHOP", "esf-int-linux-CHROME-SEARCH"),
                            new StringParameterValue("TESTGROUPS", "SEARCH")]
      esf.runEsfTests(esfParameters)
    },
    "US" : {
      def esfParameters = baseParameter + [
                            new StringParameterValue("SHOP", "esf-int-linux-CHROME-US"),
                            new StringParameterValue("TESTGROUPS", "US")]
      esf.runEsfTests(esfParameters)
    },
    "USTAX" : {
      def esfParameters = baseParameter + [
                            new StringParameterValue("SHOP", "esf-int-linux-CHROME-USTAX"),
                            new StringParameterValue("TESTGROUPS", "USTAX")]
    },
    "ERPShop" : {
      def esfParameters = baseParameter + [
                            new StringParameterValue("SHOP", "esf-int-linux-CHROME-ERPShop"),
                            new StringParameterValue("TESTGROUPS", "ERPShop")]
      esf.runEsfTests(esfParameters)
    },
    "eCMS" : {
      def esfParameters = baseParameter + [
                            new StringParameterValue("SHOP", "eCMSFree"),
                            new StringParameterValue("TESTGROUPS", "eCMS")]
      esf.runEsfTests(esfParameters)
    }
  )
}

/**
 * Test all changed esfEpages test against unstable-main.
 *
 * @param branch we want to test the esf.
 */
def testChanged(String branch) {

  stage("Test affected tests") {
    try { 
      String changedFilesFile = "changed_files_in_pr.txt"
      String affectedTestsFile = "affected_tests_by_pr.txt"
      echo "Find all changed files..."
      sh """
        set +x -e
        export TOP_OF_UPSTREAM=`git merge-base HEAD origin/pre-release-development`
        echo -n '' > \$(pwd)/${changedFilesFile}
        git diff --name-only --numstat HEAD \${TOP_OF_UPSTREAM} | sort --unique >> \$(pwd)/${changedFilesFile}
      """
      String changedFiles = readFile changedFilesFile
      echo changedFiles.trim()
      echo "Find all affected tests..."
      sh """
        set +x -e
        echo -n '' > \$(pwd)/${affectedTestsFile}
        python \$(pwd)/get_affected_tests.py -f \$(pwd)/${changedFilesFile} -p \$(pwd) | xargs echo | sed -e 's| |,|g' >> \$(pwd)/${affectedTestsFile}
      """
      String affectedTests = readFile affectedTestsFile
      echo affectedTests.trim()
      // Don't start a test if there are no affected tests.
      if (!affectedTests.trim().isEmpty()) {
        def esfParameters = [
          new StringParameterValue("TARGET_DOMAIN", "schwarzbart.epages.systems"),
          new StringParameterValue("DOCKER_TAG", "epages/esf:" + branch),
          new StringParameterValue("SKIPPRECONDITIONS", "true"),
          new StringParameterValue("RESTART_BROWSER", "true"),
          new StringParameterValue("TEST_CLASS", affectedTests),
          new StringParameterValue("TESTGROUPS", "CORE,SEARCH"),
          new LabelParameterValue("NODE_NAME", "description", env.NODE_NAME)]
        esf.runEsfTests(esfParameters)
      }
    }
    finally {
      echo "Clean up workspace, Removing old containers, images..."
      deleteDir()
      sh """
        set +x -e
        docker ps -a | grep -v 'CONTAINER ID' | grep -v -E ' (seconds|minutes|hours) ' | awk '{ print \$1 }' | sudo xargs docker rm -f || true
        docker images | grep -E '(testpr|testfromforkedrepo|<none>)' | grep -v -E ' (seconds|minutes|hours) ' | awk '{ print \$3 }' | sudo xargs docker rmi || true
      """
    }
  }
}

/**
 * Build the project esfEpages with gradle in an docker container. This build doesn't call
 * the sonar runner and is used for pull request testing.
 *
 * @param branch we want to build the esf for.
 */
def buildChanged(String branch) {

  stage("Build ${branch}") {
    sh """
      chmod +x gradlew
      docker run --rm -t -v ${pwd()}:/opt/build -v ${pwd()}/esf-epages6:/opt/esf-epages6 \
      epages/oracle-jdk8-fontconfig-freetype:latest /bin/bash -c 'cd /opt/build; ./gradlew clean dist;'
      docker build -t epages/esf:${branch} .
    """
  }
}

/**
 * Publish the finished esfEpages docker image to docker hub.
 *
 * @param branch we want to publish.
 */
def publish(String branch, String dockerCredentials) {

  stage("Publish") {
    withCredentials([[
      $class: 'UsernamePasswordMultiBinding',
      credentialsId: dockerCredentials,
      passwordVariable: 'PASSWORD',
      usernameVariable: 'USER']]) {
        sh """
          set +x ; docker login -u ${env.USER} -p ${env.PASSWORD} ; set -x
          docker push epages/esf:${branch}
        """
      }
  }
}

/**
 * Merge something e.g.: from 'dev' to 'master' or 'master' to 'stable'.
 *
 * @param originBranch branch to merge.
 * @param targetBranch branch to be merged in.
 * @param credentials credentials for the merge.
 */
def merge(String originBranch, String targetBranch, String credentials) {

  stage("Merge ${originBranch} into ${targetBranch}") {
    echo "We want to merge from ${originBranch} to ${targetBranch}"
  }
}
