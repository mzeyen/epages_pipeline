#!/usr/bin/env groovy

// "patchWrapper" job parameter. 
String repo = getProperty("REPO")
String version = getProperty("VERSION")
String previousVersion = getProperty("PREVIOUS_VERSION")

// "patch*" jobs parameter.
def parameter = [
  new StringParameterValue("REPO", repo),
  new StringParameterValue("VERSION", version),
  new StringParameterValue("PREVIOUS_VERSION", previousVersion)
]

// "patch*" jobs
def patchJobs = [
  "patch/patchAmCentOs7",
  "patch/patchDistributedCentOs6",
  "patch/patchFlexCentOs6",
  //"patch/patchHistoryCentOs6",
  "patch/patchHotfixCentOs6",
  "patch/patchMultistoreCentOs6",
  "patch/patchSingleHostCentOs6",
  "patch/patchSingleHostCentOs7",
  "patch/patchSingleHostDebian7",
  "patch/patchSingleHostDebian8",
  "patch/patchUnityCentOs7"
]

def parallelJobs = [:]

for (int i = 0; i <  patchJobs.size(); i++) {
  // Because of reasons: http://blog.freeside.co/2013/03/29/groovy-gotcha-for-loops-and-closure-scope/
  def index = i
  parallelJobs["patch${i}"] = {
    stage(patchJobs[index]) {
      build job: patchJobs[index],
            wait: false,
            parameters: parameter
    }
  }
}

parallel parallelJobs
