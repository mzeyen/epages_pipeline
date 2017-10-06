#!/usr/bin/env groovy

/**
 * Start the PR test for MultiStore.
 *
 * 1. Build EMS, RTG and EP6 from the given branches.
 * 2. Test EMS via EMS.
 * 3. Test EMS via RTG.
 *
 * @params ep6Remote The remote where the Cartridges should come from.
 * @params ep6Branch The branch where the Cartridges should come from
 * @params emsRemote The remote where the EMS sources should come from.
 * @params emsBranch The branch where the EMS sources should come from.
 * @params rtgRemote The remote where the RTG sources should come from.
 * @params rtgBranch The branch where the RTG sources should come from.
 */
node('team-gold-demo-ms') {
  try {
    stage("Get variables from pre-job...") {
      String ep6Remote = getProperty("ep6Remote")
      String ep6Branch = getProperty("ep6Branch")
      String emsRemote = getProperty("emsRemote")
      String emsBranch = getProperty("emsBranch")
      String rtgRemote = getProperty("rtgRemote")
      String rtgBranch = getProperty("rtgBranch")
    }

    // Start stage for preparing and installing the environment regarding Pr test
    stage("Build EMS, RTG, EP6") {
      parallel (
        // This job starts job from file jobs/install/installEMS.groovy
        "ems" : {
          build job: "Install_EMS_from_branch", parameters: [
                [$class: 'StringParameterValue', name: 'installVM', value: 'team-gold-demo-ms'],
                [$class: 'StringParameterValue', name: 'branch', value: emsBranch],
                [$class: 'StringParameterValue', name: 'repository', value: 'multistore-MS'],
                [$class: 'StringParameterValue', name: 'remote', value: emsRemote]]
        },
        // This job starts job from file jobs/install/installRTG.groovy
        "rtg" : {
          build job: "Install_RTG_from_branch", parameters: [
                [$class: 'StringParameterValue', name: 'installVM', value: "team-gold-demo-ms"],
                [$class: 'StringParameterValue', name: 'branch', value: rtgBranch],
                [$class: 'StringParameterValue', name: 'repository', value: 'rest-test-gold'],
                [$class: 'StringParameterValue', name: 'remote', value: rtgRemote]]
        },
        // This job starts job from file jobs/install/installMultistoreFromBranch.groovy
        "ep6" : {
          build job: "Install_EP6_from_branch", parameters: [
                [$class: 'StringParameterValue', name: 'installVM', value: "team-gold-demo-ep"],
                [$class: 'StringParameterValue', name: 'branch', value: ep6Branch],
                [$class: 'StringParameterValue', name: 'repository', value: 'Cartridges'],
                [$class: 'StringParameterValue', name: 'remote', value: ep6Remote]]
        }
      )
    }

    // Start with tests from here. Make sure to start them in sequence to avoid errors coming from parallel testing
    // Make EMS intern tests
    stage("Make code validation tests for EMS") {
      // This job starts job from file jobs/tests/runEMSTests.groovy
      build job: "Test_EMS_after_install", parameters: [
            [$class: 'StringParameterValue', name: 'installVM', value: 'team-gold-demo-ms'],
            [$class: 'StringParameterValue', name: 'containerName', value: 'multistore-lb-production']]
    }

    // Make EMS integration tests from RTG
    stage("Test EMS with sources from RTG") {
      // This job starts job from file jobs/tests/runRTGTests.groovy
      build job: "Test_EMS_from_RTG", parameters: [
            [$class: 'StringParameterValue', name: 'installVM', value: 'team-gold-demo-ms'],
            [$class: 'StringParameterValue', name: 'msServerUrl', value: 'http://team-gold-demo-ms.vm-intern.epages.com']]
    }
          
    /**
     * We have to add here more tests. We could use RTG tests for ep6 as well as perl module tests on ep6 side.
     * This is definatly a TODO!
     */
    currentBuild.result = 'SUCCESS'
  } catch (Exception) {
    currentBuild.result = 'FAILURE'
  } finally {
      // Send a mail with the build result to the members of team gold here.
      mail (to: 'f.ziller@epages.com a.grohmann@epages.com',
            subject: '[PR Test] Your Pull request ended with ' + currentBuild.result + '.',
            body: "Please go to ${env.BUILD_URL} and check the results.\n\nThanks,\nTeam Gold")
  }
}