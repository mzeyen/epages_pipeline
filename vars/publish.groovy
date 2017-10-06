#!/usr/bin/env groovy

/**
 * Publish repo.
 *
 * @param version The version for publishing.
 */
def publishRepo(String version) {

    stage("Publish repo") {
        sh """
          export BUILD_ID=dontKillMe
          ~/epages6-packages/scripts/PublishRepos.sh -b $version
        """
    }
}

/**
 * Delete repos.
 *
 * @param version The version for publishing
 * @param remoteVm The remote VM where you want to delete the repos.
 */
def deleteRepo (String version, String remoteVm = "") {

    stage("Delete repos") {
        if (remoteVm == "") {
            sh "~/epages-infra/scripts/ftp-rsync/cdp-delete-repos.sh -r $version -d /home/build/repo -x 1"
        }
        else {
            sh "ssh $remoteVm \"~/epages-infra/scripts/ftp-rsync/cdp-delete-repos.sh -r $version -d /home/build/repo -x 1\""
        }
    }
}

/**
 * Tag repos after publish
 *
 * @param version Version for tag.
 */
def tagGitAfterPublish (String version) {

    stage("Tag git repos after publish") {
        sh "~/epages-infra/scripts/epages/tag/tag_git_after_publish.sh -v $version"
    }
}

/**
 * Run publish test jobs.
 *
 * @param parameter Parameters for the test jobs.
 */
def runTestsAfterPublish (String parameter) {

    stage("Start publish test jobs") {
        build job: "publish/publishTestWrapper", parameters: parameter
    }
}
