#!/usr/bin/env groovy

// "publishTestWrapper" job parameter.
String previousVersion = getProperty("PREVIOUS_VERSION")
String version = getProperty("VERSION")

// "publish*" jobs parameter.
def parameter = [
        new StringParameterValue("PREVIOUS_VERSION", previousVersion),
        new StringParameterValue("VERSION", version)
]

// "publishTest" jobs
def publishTest = [
        "publish/publishPatchTest",
        "publish/installTestPublishedVersion",
        "publish/publishPublicInstallTest"
]

def parallelJobs = [:]

for (int i = 0; i <  publishTest.size(); i++) {

    parallelJobs["publish${i}"] = {
        stage(publishTest[i]) {
            build job: publishTest[i], parameters: parameter
        }
    }
}

parallel parallelJobs
