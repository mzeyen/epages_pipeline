#!/usr/bin/env groovy

/**
 * Create fingerprint.
 */
def createFingerprint(String filesToExclude = "") {

  stage("create fingerprint") {
    String excludedFiles = ""
    if (filesToExclude != "") {
      excludedFiles = filesToExclude
    }
    sh """
      export BUILD_ID=dontKillMe
      export EXCLUDE_FILES_IN_NORMALIZE=${excludedFiles}
      ~/epages-infra/scripts/epages/fingerprint/create_fingerprint.sh -a
    """
  }
}

/**
 * Create fingerprint for distributed installations.
 */
def createFingerprintDistributed(String filesToExclude = "") {
  
  stage("create fingerprint") {
    String excludeFiles = ""
    String configuration = "/srv/epages/etc/sysconfig/simple/install-epages-distributed.conf"
    if (filesToExclude != "") {
      excludeFiles = filesToExclude
    }
    sh """
      export BUILD_ID=dontKillMe
      /var/epages/fingerprint/fingerprint.sh --conf ${configuration} --drop-normalize ${excludeFiles} --checksum sha1 
    """
  }
}

/**
 * Create fingerprint for distributed installations - patch - needs refactoring.
 */
def createFingerprintDistributedPatch(String filesToExclude = "") {
  
  stage("create fingerprint") {
    String excludeFiles = ""
    if (filesToExclude != "") {
      excludeFiles = filesToExclude
    }
    sh """#!/bin/bash
      export BUILD_ID=dontKillMe
      CFG=/srv/epages/etc/sysconfig/simple/install-epages-distributed.configuration
      sed 's,install-,patch-,' \$CFG > \${CFG/install-/patch-}
      /var/epages/fingerprint/fingerprint.sh --conf \${CFG/install-/patch-} --drop-normalize ${excludeFiles} --checksum sha1
    """
  }
}

/**
 * Copy fingerprint.
 *
 * @param version The version of epages installation/patch
 * @param repo The repo of the epages installation/patch
 * @param envIdentifier The identifier of the environment (e.g.: single host/distributed)
 * @param envType The type of the environment (e.g.: install/patch)
 */
def copyFingerprint(String version, String repo, String envIdentifier, String envType, String sourceDir = "/tmp/epages-installation-fingerprint") {

  stage("copy fingerprint") {
    sh """
      export BUILD_ID=dontKillMe
      ~/epages-infra/scripts/epages/fingerprint/copy_fingerprint.sh -v ${version} -r ${repo} -i ${envIdentifier} -t ${envType} -s ${sourceDir}
    """
  }
}
