#!/usr/bin/env groovy

// "qaWrapper" job parameter. 
String repo = getProperty("REPO")
String version = getProperty("VERSION")

// "qa*" jobs parameter.
def parameter = [
  new StringParameterValue("REPO", repo),
  new StringParameterValue("VERSION", version)
]

// "qa*" jobs
def qaJobs = [
  "qa/qa1stLevelCentOs6",
  "qa/qa1stLevelDebian8",
  "qa/qaEsfExternalDebian8",
  "qa/qaEsfInternalCentOs6",
  "qa/qaEsfInternalDebian7",
  "qa/qaPerlCriticCentOs6",
  "qa/qaQuarantine",
  "qa/qaSoapWsCentOs6",
  "qa/qaSoapWsDebian7",
  "qa/qaUnitCentOs6",
  "qa/qaUnitDebian8"
]

def parallelJobs = [:]

for (int i = 0; i <  qaJobs.size(); i++) {
  // Because of reasons: http://blog.freeside.co/2013/03/29/groovy-gotcha-for-loops-and-closure-scope/
  def index = i
  parallelJobs["qa${i}"] = {
    stage(qaJobs[index]) {
      build job: qaJobs[index],
            wait: false,
            parameters: parameter
    }
  }
}

parallel parallelJobs
