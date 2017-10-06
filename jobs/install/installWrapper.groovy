#!/usr/bin/env groovy

// "installWrapper" job parameter. 
String repo = getProperty("REPO")
String version = getProperty("VERSION")

// "install*" jobs parameter.
def parameter = [
  new StringParameterValue("REPO", repo),
  new StringParameterValue("VERSION", version)
]

// "install*" jobs
def installJobs = [
  "install/installAmCentOs7",
  "install/installDistributedCentOs6",
  "install/installFlexCentOs6",
  "install/installMultistoreCentOs6",
  "install/installSingleHostCentOs6",
  "install/installSingleHostCentOs7",
  "install/installSingleHostDebian7",
  "install/installSingleHostDebian8",
  "install/installUnityCentOs7",
  "install/installUnityDebian8"
]

def parallelJobs = [:]

for (int i = 0; i <  installJobs.size(); i++) {
  // Because of reasons: http://blog.freeside.co/2013/03/29/groovy-gotcha-for-loops-and-closure-scope/
  def index = i
  parallelJobs["install${i}"] = {
    stage(installJobs[index]) {
      build job: installJobs[index],
            wait: false,
            parameters: parameter
    }
  }
}

parallel parallelJobs
