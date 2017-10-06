#!/usr/bin/env groovy

/**
 * Get artefacts from other Job.
 */
def getArtefactFromJob(String jobName, String buildId, String filter) {

  stage('Get artefacts from other Job.') { 
    step ([$class: 'CopyArtifact',
        projectName: jobName,
        filter: filter,
        fingerprintArtifacts: true,
        target: jobName,
        flatten: true,
        selector: [$class: 'SpecificBuildSelector', buildNumber: buildId]
        ]);
  }
}

/**
 * Archive artefacts.
 *
 * @param artefact we want to archive
 */
def archiveFiles(String artefact) {

  stage('Archive specified file.') {
    archiveArtifacts artefact
  }
}
