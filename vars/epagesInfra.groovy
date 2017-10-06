#!/usr/bin/env groovy

/**
 * Deploy epages infra on the test VM (e.g.: if puppet is not working correct).
 */
def deployEpagesInfra() {

  stage("Deploy ePages infra") {
    sh """
      if [ ! -d ~/epages-infra/.git ] ; then
        cd ~
        git clone git@github.com:ePages-de/epages-infra.git
      else
        cd ~/epages-infra && git pull origin master
      fi
    """
  }
}
