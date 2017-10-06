#!/usr/bin/env groovy

/**
 * Run ESF tests with a given configuration.
 *
 * Call with list of ParameterValues
 * e.g.: def esfParameters = [
 *          new StringParameterValue("TARGET_DOMAIN", "unstable-main.epages.com"),
 *          new StringParameterValue("VERSION", "nightly"),
 *
 * esf.runEsfTests(esfParameters)
 */
def runEsfTests(esfParameters) {

  boolean runQuaratine = false
  def testGroup
  for (int i = 0; i < esfParameters.size(); i++) {
    if (esfParameters.get(i).getName() == "TESTGROUPS") {
      testGroup = esfParameters.get(i).getValue()
    }
    if (esfParameters.get(i).getName() == "RUN_QUARANTINE_TESTS") {
      runQuaratine = true
    }
  }
  stage ("Run ESF Tests: " + testGroup) {
    try{
      build job: "Run_ESF_tests", parameters: esfParameters
    }
    catch (Exception e){
      if (!runQuaratine) {
        throw e
      }
    }
  }
}

/**
 * Run ESF tests with a given configuration.
 *
 * Call with list of ParameterValues
 * e.g.: def esfParameters = [
 *          new StringParameterValue("TARGET_DOMAIN", "unstable-main.epages.com"),
 *          new StringParameterValue("VERSION", "nightly"),
 *
 * esf.runEsfTests(esfParameters)
 */
def runEsfUnityTests(esfParameters) {

  def testGroup
  for (int i = 0; i < esfParameters.size(); i++) {
    if (esfParameters.get(i).getName() == "GROUPS") {
      testGroup = esfParameters.get(i).getValue()
    }
  }
  stage("Run ESF Unity Tests: " + testGroup) {
    build job: "Run_ESF_UNITY_tests", parameters: esfParameters
  }
}
