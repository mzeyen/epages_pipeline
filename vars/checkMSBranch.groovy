#!/usr/bin/env groovy

@groovy.transform.Field prId = "${env.BRANCH_NAME}".substring(3)

/**
 * This method will start the PR test for MultiStore, if a change in a MultiStore cartridge was
 * detected earlier. It will check for same branch names in other repositories and trigger the test,
 * if they don't exist. Else the Jenkinsfile of another repository will trigger the test.
 */
def triggerMultiStorePRTestForCartridges() {

  try {
    // Create necessary variables
    // This variable will be needed to assign it to other variables.
    String BRANCH
    String REMOTE

    stage("Get branch names and start PR test...") {

      // If the script was started with a PR, no BRANCH is given and we have to find it via PR.
      BRANCH = gitActions.getBranchNameFromPr("Cartridges", prId)
      EP6_BRANCH_NAME = BRANCH

      // If the script was started with a PR, no REMOTE is given and we have to find it via PR.
      REMOTE = gitActions.getRemoteNameFromPr("Cartridges", prId)
      EP6_REMOTE_NAME = REMOTE

      // We now have to evaluate, if branches exist in the named repository.
      if (gitActions.getBranchForRepository("multistore-MS", BRANCH, REMOTE)) {
        // If a branch with the same name exist in multistore-MS, we can exit here,
        // as the PR test will be triggered by Jenkinsfile of multistore-MS
        println "We exit here. Repository multistore-MS will take care of the PR test..."
        currentBuild.result = 'SUCCESS'
        return
      } else {
        // We have to set the branch and remote name to the standard, if branch do not exist.
        println "We can use dev for multistore-MS..."
        EMS_BRANCH_NAME = 'dev'
        EMS_REMOTE_NAME = 'ePages-de'
      }

      // We now have to evaluate, if branches exist in the named repository.
      if (gitActions.getBranchForRepository("rest-test-gold", BRANCH, REMOTE)) {
        // If a branch with the same name exist in multistore-MS, we can exit here,
        // as the PR test will be triggered by Jenkinsfile of multistore-MS
        println "We exit here. Repository rest-test-gold will take care of the PR test..."
        currentBuild.result = 'SUCCESS'
        return

      } else {
        // We have to set the branch and remote name to the standard, if branch do not exist.
        println "We can use develop for rest-test-gold..."
        RTG_BRANCH_NAME = 'dev'
        RTG_REMOTE_NAME = 'ePages-de'
      }

      // Trigger the PR test for Multistore and end this job
      build job: "PR_Test", 
            wait: false,
            parameters: [[$class: 'StringParameterValue', name: 'ep6Branch', value: EP6_BRANCH_NAME],
                         [$class: 'StringParameterValue', name: 'ep6Remote', value: EP6_REMOTE_NAME],
                         [$class: 'StringParameterValue', name: 'emsBranch', value: EMS_BRANCH_NAME],
                         [$class: 'StringParameterValue', name: 'emsRemote', value: EMS_REMOTE_NAME],
                         [$class: 'StringParameterValue', name: 'rtgBranch', value: RTG_BRANCH_NAME],
                         [$class: 'StringParameterValue', name: 'rtgRemote', value: RTG_REMOTE_NAME]]
      currentBuild.result = 'SUCCESS'
    } catch (e) {
      currentBuild.result = 'FAILURE'
      throw e
    }
  }
}
