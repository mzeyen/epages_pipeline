#!/usr/bin/env groovy

import groovy.json.JsonSlurperClassic

/**
 * Clone a given git repository.
 *
 * @params remote The remote, from which should be cloned.
 * @params repository The repository which should be cloned.
 */
def clone(String remote, String repository) {
  stage('Clone repo') {
    sh "git clone git@github.com:${remote}/${repository}.git ${repository}"
  }
}

/**
 * Clone a given git repository and use a given branch.
 *
 * @params remote The remote, from which should be cloned.
 * @params repository The repository which should be cloned.
 * @params branch The branch, which should be cloned.
 */
def cloneFromBranch(String remote, String repository, String branch) {
  stage('Clone repo from branch') {
    sh "git clone -b ${branch} git@github.com:${remote}/${repository}.git ${repository}"
  }
}

/**
 * Pull a specific branch from a repository.
 *
 * @params remote The remote, from which should be pulled.
 * @params repository The repository, from which should be pulled.
 * @params branch The branch, from which should be pulled.
 */
def pull(String remote, String repository, String branch) {
  stage('Pull from repository') {
    sh "echo 'Pulling Branch ${branch} from ${remote} ...'"
    sh "cd ${repository} && git pull ${remote} ${branch}"
  }
}

/**
 * Get the name of the branch from a given pull request ID.
 *
 * @param repository The repository, where the pull request can be found.
 * @param changeID The ID from the pull request.
 * @return String The name of the branch from the given pull request.
 */

def String getBranchNameFromPr(String repository, String changeID) {
  def getBranchNameURL = 'https://api.github.com/repos/ePages-de/' + repository + '/pulls/' + changeID
  return this.getFromGithubAPI(getBranchNameURL).head.ref
}

/**
 * Get the name of the remote from a given pull request ID.
 *
 * @param repository The repository, where the pull request can be found.
 * @param changeID The ID from the pull request.
 * @return String The name of the remote from the given pull request.
 */

def String getRemoteNameFromPr(String repository, String changeID) {
  def getBranchNameURL = 'https://api.github.com/repos/ePages-de/' + repository + '/pulls/' + changeID
  return this.getFromGithubAPI(getBranchNameURL).head.repo.owner.login
}

/**
 * Check, if a branch with given name exists on the given remote for the given repository.
 *
 * @param repository The name of the repository, where the branch should exist.
 * @param branch The name of the branch, which should be checked.
 * @param remote The name of the remote, where the branch should exist.
 * @return boolean Return true, if branch exist, else false.
 */
def boolean getBranchForRepository(String repository, String branch, String remote) {

  response = this.getFromGithubAPI("https://api.github.com/repos/" + remote + "/" + repository + "/branches/" + branch)
  echo response

  try {
    // If response.name is not define b/c of not existent, the catch block will return false, else true
    return response.name == branch
  } catch(Exception e) {
    // Return false in case something goes wrong
    return false
  }
}

/**
 * Get a URL from Github and return the responded JSON.
 *
 * @param URL The URL, which should get called via a GET call.
 * @return JSON Return the JSON, which was responded from github.
 */

def getFromGithubAPI(String url) {

  withCredentials([[$class: 'StringBinding', credentialsId: 'MS_Credentials_PR', variable: 'GITHUB_API_TOKEN']]) {
    try {
      gitAPIURLConnection = url.toURL().openConnection()
      gitAPIURLConnection.setRequestMethod("GET")
      gitAPIURLConnection.setDoInput(true)
      gitAPIURLConnection.setRequestProperty('Authorization', 'token ' + env.GITHUB_API_TOKEN)

      if ( gitAPIURLConnection.responseCode == 401 ){
        Error("Unauthorized - Check credentials for Github.")
      } else {
        text = gitAPIURLConnection.content.text
        return new JsonSlurperClassic().parseText( text )
      }
    } catch(Exception e) {
      // Return empty string, if something goes wrong
      return ""
    } finally {
      // Avoid java.io.NotSerializableException: sun.net.www.protocol.https.HttpsURLConnectionImpl.
      gitAPIURLConnection = null
    }
  }
}

// Important for the Pipeline Remote Loader Plugin and build-in load. Avoid null pointer exception.
return this
