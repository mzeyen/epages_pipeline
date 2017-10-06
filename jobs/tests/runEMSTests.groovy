#!/usr/bin/env groovy

String installVM = getProperty("installVM")
String containerName = getProperty("containerName")

node("${installVM}") {
  try {
    // Run rspec tests
    stage "Run RSpec tests"
      sh 'docker exec ' + containerName + ' /bin/bash -c "RAILS_ENV=production bundle exec rspec spec"'
      sh 'docker exec ' + containerName + ' /bin/bash -c "RAILS_ENV=production bundle exec rake test"'
  } catch (exception) {
    throw exception
  }
}
