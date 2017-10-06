#!/usr/bin/env groovy

// VMs
String patchVM = "team-gold-demo-ep"
String version

/**
 * Patch - Normal - MultiStore - CentOS6:
 * 1. Run puppet agent test.
 * 2. Patch.
 * 3. Send mail.
 * 4. Trigger EMS Tests.
 */
node("${patchVM}") {
    // Get version, which was overhanded
    if (getBinding().hasVariable("VERSION")) {
        version = getProperty("VERSION")
    }
    checkout scm
    def patch = load("jobs/modules/patch.groovy")
    try {
        stage("Initilization")
        sh "echo 'Start here...'"
        patch.patchToReleasedVersion(version)
    } catch (exception) {
        throw exception
    } finally {
        currentBuild.result = "${currentBuild.result}"
        String recipient = 'f.ziller@epages.com'
        mail subject: "${env.JOB_NAME} (${env.BUILD_NUMBER}) failed",
            body: "It appears that ${env.BUILD_URL} is failing, somebody should do something about that",
            to: recipient,
            replyTo: recipient,
            from: 'noreply@ci.jenkins.io'
    }
}
