#!/usr/bin/env groovy

/**
 * Run the puppet agent.
 */
def test() {
  
  stage("Run puppet agent") {
    int return_code = -1;
    try {
      sh 'puppet agent --test'
    } catch(e) {
      // Calls to Puppet Agent return 0, if nothing has changed and 2, if Puppet was successfully.
      return_code = this.getExitCode(e.message);
      if (return_code != 0 && return_code != 2) {
        throw e;
      }
    }
  }
}

/**
 * Read out the exit code number from an exception message. Helper method.
 *
 * The message parameters looks like this: "script returned exit code 2".
 *
 * REFERENCE: http://mrhaki.blogspot.de/2009/09/groovy-goodness-matchers-for-regular.html
 */
def int getExitCode(message) {

  def group = (message =~ /script returned exit code ([0-9]+)/)
  return Integer.parseInt(group[0][1]);
}
