#!/usr/bin/env groovy

String installVM = getProperty("installVM")
String msServerUrl = getProperty("msServerUrl")

node("${installVM}") {
  try {
    // Run rspec tests
    stage "Run EMS rest tests"
      sh 'docker exec rest-test-gold bundle exec /bin/bash -c "rake test MS_SERVER_URL=' + msServerUrl + ' TEST=test/REST/ems_rest/*/*.rb"'
  } catch (exception) {
    throw exception
  }
}
