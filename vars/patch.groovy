#!/usr/bin/env groovy

/**
 * Patch like AM.
 *
 * @param version The version for patching.
 */
def patchAmLike(String version) {

  stage("Patch: AM") { 
    sh """
      export BUILD_ID=dontKillMe
      ~/epages-infra/scripts/epages/am/install_am_patch.sh -r ftp://ftp.epages.com/pub/epages/build/repo/${version}/latest
    """
  }
}

/**
 * Patch like RND.
 * 
 * @param version The version for patching.
 * @param isDistributed default false, true if you want to patch a distributed installation.
 */
def patch(String version, Boolean isDistributed = false) {

  stage("Patch: Patch API") { 
    String distributedFlag = ""
    if (isDistributed) {
      distributedFlag = "-d"
    }
    sh """
      export BUILD_ID=dontKillMe
      ~/epages-infra/scripts/epages/install_patch.sh -r ftp://ftp.epages.com/pub/epages/build/repo/${version}/latest ${distributedFlag}
    """
  }
}

/**
 * Patch a given step.
 *
 * @param version The version for patching
 * @param patchStep The oatch step e.g.: prepare, preinstall, install, postinstall, reopen
 */
def callPatchStep(String version, String patchStep) {

  stage("Call patch step: ${patchStep}") {
    sh """
      export BUILD_ID=dontKillMe
      export PATCH_STEP=${patchStep}
      ~/epages-infra/scripts/epages/di/di_call_patch.sh -s ${patchStep} -v ${version}
    """
  
  }
}

/**
 * Patch to the last released verssion.
 *
 * @param version The version for patching
 */
def patchToReleasedVersion(String version) {

  def git = load("jobs/modules/git.groovy")
  try {
    git.clone("ePages-de", "epages-infra")
  } catch (e) {
    git.pull("ePages-de", "epages-infra", "dev")
  }
  sh "epages-infra/scripts/epages/install_patch.sh -r ${version}/latest"
}

/**
 * Patch Flex store.
 * 
 * @param flexStoreVm The Flex VM with Store.
 */
def patchFlexStore(String flexStoreVm, String version) {

  stage("Patch FlexStore VM: ") {
    sh """
      if [[ ${version} == \"7.11.0\" ]] ; then
        ansible-playbook /var/epages/flex/ansible/migrate.yml --limit ${flexStoreVm} -vvv
        ansible-playbook /var/epages/flex/ansible/patch.yml -vvv
      else
        ansible-playbook /var/epages/flex/ansible/patch.yml -vvv
      fi
    """
  }
}

/**
 * Patch to published version.
 *
 * @param version The published version.
 */
def patchToPublishedVersion(String version) {

    stage("Patch to published version $version") {
        sh """
            export EPAGES_REPOURL=ftp://epages-software.de/repo/usr/0914cf1e-0d62-47c8-8ca1-14e4ca3ef036
            /var/epages/SetRepository.sh -b $version
            /var/epages/update-epages.sh --singlehost --repo \$EPAGES_REPOURL
        """
    }
}

/**
 * Remove Shared/Patches folder.
 */
def removePatchesFolder() {

    stage ("Remove Shared/Patches folder") {
        sh """
            . /etc/default/epages6
            if [[ -d \$EPAGES_SHARED/Patches/ ]] ; then
                echo 'Removing old patch files form Shared/Patches...'
                rm -Rf \$EPAGES_SHARED/Patches/*
            fi
        """
    }
}
