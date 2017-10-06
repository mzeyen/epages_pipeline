#!/usr/bin/env groovy

import groovy.json.JsonSlurper;

infraDir = "/root/epages-infra"
esfTestShop = "DemoShop${env.BUILD_NUMBER}"
epRoot = "/srv/epages/eproot"
prId = env.CHANGE_ID

/**
 * Running puppet agent test for maintenance. Config stage.
 */
def runningPuppetAgent() {

  stage("CONFIG: Running Puppet agent") {
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
 * Checkout the current branch for a multibranch project and rebase it to origin(ePages-de)/develop. Config stage.
 */
def checkout() {

  stage("CONFIG: Checkout and rebase ${env.BRANCH_NAME}") {
    sh "git config --global credential.helper cache"
    checkout scm
    // Repair broken rebases.    
    sh "rm -rf ${pwd()}/.git/rebase-apply"
    // Rebase 
    sh "git fetch origin"
    sh "git rebase origin/develop"
  }
}

/**
 * Update sources. Config stage.
 */
def updateSources() {

  stage("CONFIG: Copy updated sources to Cartridge directory") {
    withEnv(["BUILD_ID=dontKillMe"]) {
      // We need an epages installation, if it is not present (AD-4969)
      echo "This is the workspace: ${pwd()}"
      sh "${infraDir}/scripts/epages/reinstall-update-cartridges.sh -s ${pwd()}"
    }
  }
}

/**
 * Restart epages6 service and solr. Config stage.
 *
 * REFERENCE: https://epages.atlassian.net/browse/AD-4862
 */
def restartServices() {

  stage("TEST: Restart services") {
    withEnv(["BUILD_ID=dontKillMe"]) {
      try {
        echo "Restarting epages6 and solr services."
        sh "service epages6 start"
        sh "service mongodb restart"
        sh "service epages-solr-slave restart"
        sh "service epages-solr restart"
        sh "service epagesj restart"
      } catch (e) {
        echo "Services are running fine."
      }
    }
  }
}

/**
 * Config App Servers.
 */
def configAppServers() {

  stage("CONFIG: Reconfigure App Servers and stop ePages cron jobs") {
    withEnv(["BUILD_ID=dontKillMe"]) {
      sh "${infraDir}/scripts/epages/configure_app_servers.sh -m 390 -n 3 -r"
      sh '/etc/init.d/epages6 stop_cron'
    }
  }
}

/**
 * Send mail to committer, if a stage has errors. Config stage.
 *
 * Reference: https://epages.atlassian.net/browse/AD-4946
 */
def sendSuccessMailToCommiter() {

  stage("CONFIG: Send success mail") {  
    withCredentials([[
      $class: 'StringBinding', 
      credentialsId: '0204e46d-eb6f-481d-be54-ad1ed103252b',
      variable: 'jiraToken']]) {
          sh "${infraDir}/scripts/epages/github/get_infos_from_git_API.sh -p ${prId} -t ${env.jiraToken}"
    }

    String mailAddress = readFile("/tmp/pr_test_mail_adress.txt")
    String prTitle = readFile("/tmp/${prId}.title.txt")
    
    String chuck = this.getRandomChuckJoke();
    mail (to: "$mailAddress",
          subject: "[PR Test] Your Pull request ${prTitle} was successful.",
          body: "Your Pull Request\n${prTitle}was successful.\n(see ${env.BUILD_URL})\nGood work.\n\nSincerely,\nTeam Orange\n\n----------\n\n${chuck}");
    echo "Pull Request succeeded. Mail was sent. Have a nice day. :)"
  }
}

/**
 * Checking the git commits for JIRA IDs.
 *
 * The path we use now is valid only for PR test nodes (cd-vm-test-pr-[0-9]{3}) and may
 * be different elsewhere.
 *
 * REFERENCE: http://<YOUR RUNNING INSTANCE>/workflow-cps-snippetizer/dslReference
 */
def checkingGitCommits() {
  
  String stageName = "TEST: Checking Git commits"
  stage(stageName) {
    try {
      sh "${infraDir}/scripts/epages/test/check_git_commits.sh -d ${pwd()} -m"
    } catch (e) {
      this.sendMailToCommitter(stageName)
      throw e;
    }
  }
}

/**
 * Check all line endings. Test stage.
 *
 * @param changedFiles String with all changed files.
 */
def checkLineEndings(String changedFiles) {

  String stageName = "TEST: Check line endings"
  stage(stageName) {
    try {
      sh "${infraDir}/scripts/epages/test/check_line_endings.sh -s \"${changedFiles}\""
    }
    catch(e) {
      this.sendMailToCommitter(stageName)
      throw e;
    }
  }
}

/**
 * Check all line endings. Test stage.
 *
 * @param changedPerlFiles String with all changed perl files.
 */
def perlCritic(String changedPerlFiles) {

  String stageName = "TEST: Perl critic"
  stage(stageName) {
    try {
      sh "${infraDir}/scripts/epages/test/run_perl_critic_test.sh -s \"${changedPerlFiles}\""
    } catch (e) {
      this.sendMailToCommitter(stageName)
      throw e;
    }
  }
}

/**
 * Reinstall epages from (updated) source. Test stage.
 *
 * @param additionalCartridges additionalCartridges we want to install.
 *
 * NOTE: We have to uninstall MultiStore first to avoid conflicts building Storetypes.
 * REFERENCE: https://epages.atlassian.net/browse/AD-4805
 */
def reinstallEpages(String additionalCartridges) {
  
  String stageName = "TEST: Reinstall ePages with updated sources"
  stage(stageName) {
    withEnv(["BUILD_ID=dontKillMe"]) {
      try {
        echo "Uninstall possible MultiStore installation first..."
        sh "cd ${epRoot}/Cartridges/DE_EPAGES &&  ${epRoot}/Perl/bin/perl Makefile.PL && make uninstall_multistore"
        sh "export SKIP_ALIVE_CHECK=1; ${infraDir}/scripts/epages/reinstall.sh -s /root/jenkins/workspace"
        
        // We need to install additional cartridges, if we have changes there.
        if (additionalCartridges) {
          echo "Installing additional cartridges..."
          sh "${infraDir}/scripts/epages/install_all_additional_cartridges.sh -c ${additionalCartridges}"
        }
      } catch (e) {
        this.sendMailToCommitter(stageName)
        throw e;
      }
    }
  }
}

/**
 * Reinstall epages from (updated) source.
 *
 * NOTE: We have to uninstall MultiStore first to avoid conflicts building Storetypes.
 * REFERENCE: https://epages.atlassian.net/browse/AD-4805
 */
def reinstallEpagesMultistore() {

  String stageName = "TEST Reinstall ePages Multistore with updated sources"
  stage(stageName) {
    withEnv(["BUILD_ID=dontKillMe"]) {    
      try {
        sh "export SKIP_ALIVE_CHECK=1; ${infraDir}/scripts/epages/reinstall.sh -s /root/jenkins/workspace -m"
        echo "Installing additional cartridges for MultiStore..."
        sh "${infraDir}/scripts/epages/install_all_additional_cartridges.sh -m"
      } catch (e) {
        this.sendMailToCommitter(stageName)
        throw e;
      }
    }
  }
}

/**
 * Check if changed cartridges are present in Cartridges.xml
 *
 * @param changedCartridges String with all changed cartridges.
 *
 * REFERENCE: https://epages.atlassian.net/browse/AD-4602
 */
def checkCartridgesXML(String changedCartridges) {

  String stageName = "TEST: Check entries in Cartridges.xml"
  stage(stageName)
  withEnv(["BUILD_ID=dontKillMe"]) {
    try {
      sh "${infraDir}/scripts/epages/test/run_has_entry_in_cartridges_xml.sh -s \"${changedCartridges}\""
    } catch (e) {
      this.sendMailToCommitter(stageName)
      throw e;
    }
  }
}

/**
 * Run perl module tests. Test stage.
 *
 * @param stores array of stores to test.
 * @param changedCartridges String with all changed cartridges.
 *   default: ["Store"].
 * 
 * NOTE: Set this environment variable to skip performance tests, 
 * as they are very flaky on virtual machines, due to varying performance.
 *
 * NOTE: Do not run the tests for 3rd-party APIs as they are (for the most
 * part) very flaky too.
 * 
 * NOTE: This should be disabled in the future, but requires more
 * error prone tests.
 */
def perlModuleTests(String changedCartridges, String[] stores = ["Store"]) {

  String stageName = "TEST: Perl module tests"
  stage(stageName) {
    env.SKIP_TIMING = "yes"
    env.SKIP_EXTERNAL_TESTS = "yes"  

    // Workaround ignore tests on Site cartridges (copied from old pull request)
    String storeCartridges = "";
    for (String line : changedCartridges.split("\\s+")) {
      if ( ! ( line =~ /CodeGenerator/ || line =~ /Flex/ || line =~ /Test/ || line =~ /ShopConfiguration/ || line =~ /ShopTransferProvider/ )) {
        storeCartridges += line + " "
      }
    } 
    try {
      for (int i = 0; i < stores.length; i++) {
        if (storeCartridges != "") {
          sh "${infraDir}/scripts/epages/test/run_perl_module_tests_new.sh -s ${stores[i]} -c \"${storeCartridges}\""
          this.runTestCartridgeTests(stores[i], epRoot)
        }
      }
    } catch (e) {
      this.sendMailToCommitter(stageName)
      throw e;
    }
  }
}

/**
 * Check language tests. Test stage.
 *
 * @param changedLanguages String with all changed languages.
 * @param changedCartridges String with all changed cartridges.
 * @param stores An array of all stores to test.
 *   default: ["Store"]
 */
def checkLanguageTests(String changedLanguages, String changedCartridges, String[] stores = ["Store"]) {

  String stageName = "TEST: Check language tags for changed languages"
  stage(stageName) {
    try {
      for(int i = 0; i < stores.length; i++) {
        sh "${infraDir}/scripts/epages/test/run_check_language_tags.sh -s ${stores[i]} -l \"${changedLanguages}\" -c \"${changedCartridges}\""
      }
    } catch (e) {
      this.sendMailToCommitter(stageName)
      throw e;
    }
  }
}

/**
 * Compile templates. Test stage.
 */
def compileTemplates() {
  
  String stageName = "TEST: Compile templates"
  stage(stageName) {  
    try {
        sh "${infraDir}/scripts/epages/test/run-template-tle-test.sh"
    } catch (e) {
      this.sendMailToCommitter(stageName)
      throw e;
    }
  }
}

/**
 * Check for unused files. Test stage.
 */
def checkForUnusedFiles() {

  String stageName = "TEST: Check for unused files"
  stage(stageName) {
    try {
      sh "${infraDir}/scripts/epages/test/run_check_for_unused_files.sh"
    } catch (e) {
      this.sendMailToCommitter(stageName)
      throw e;
    }
  }
}

/**
 * Run ESF tests. Test stage.
 * 
 * @param store The store for the shop, we are testing on.
 *   default: Store.
 */
def runEsfTests(String store = "Store") {

  String stageName = "TEST: ESF SMOKETESTS"
  stage(stageName) {
    try {
      sh "${infraDir}/scripts/epages/create_shop.sh -n ${esfTestShop} -t 'Demo' -s ${store} -i"
      build job: "Run_ESF_tests", parameters: [[$class: 'StringParameterValue', name: 'TARGET_DOMAIN', value: getCurrentHostname()],
                                               [$class: 'StringParameterValue', name: 'SHOP', value: esfTestShop],
                                               [$class: 'StringParameterValue', name: 'TESTGROUPS', value: 'SMOKETEST'],
                                               [$class: 'StringParameterValue', name: 'STORE', value: store]]
    } catch (e) {
      this.sendMailToCommitter(stageName)
      throw e;
    }
  }
}

/**
 * Run webservice tests. Test stage.
 */
def webserviceTest() {

  String stageName = "TEST: Webservice SOAP Tests"
  stage(stageName) {
    try {
      String gitDiffStatFile = "/tmp/${env.BRANCH_NAME}.git_diff_stat.txt";
      sh "git diff --stat HEAD..origin/develop > ${gitDiffStatFile}"
      def wsStartCheckScript = "${infraDir}/scripts/epages/util/test_if_webservice_tests_are_required.sh"

      if (trySh("${wsStartCheckScript} -d ${gitDiffStatFile}") == 0) {
        echo "Starting SOAP API Tests..."
        sh "${infraDir}/scripts/epages/test/run_web_service_tests.sh -d \$(hostname --fqdn)"
      } else {
        echo "SOAP API Tests dont need to be started."
      }
    } catch (e) {
      this.sendMailToCommitter(stageName)
      throw e;
    }
  }
}

/**
 * Generate String of all changed files. Helper method.
 *
 * @return String containing all changed files.
 */
def String getChangedFiles() {

    String changedFilesFile = "/tmp/${env.BRANCH_NAME}.changed_files.txt";
    sh "${infraDir}/scripts/epages/pr/get_changed_files.sh -d ${pwd()} -l ${changedFilesFile}"
    return readFile(changedFilesFile).replaceAll("\r?\n", " ")
}

/**
 * Generate String of all changed language files. Helper method.
 *
 * @return String containing all changed languages.
 */
def String getChangedLanguageFiles() {

  String changedLanguageFile = "/tmp/${env.BRANCH_NAME}.changed_languages.txt";
  sh "${infraDir}/scripts/epages/pr/get_changed_languages.sh -d ${pwd()} -l ${changedLanguageFile}"
  return readFile(changedLanguageFile).replaceAll("\r?\n", " ")
}

/**
 * Generate String of all changed Cartridges. Helper method.
 *
 * @return String containing all changed Cartridges.
 */
def String getChangedCartridgeFiles() {

  String changedCartridgesFile = "/tmp/${env.BRANCH_NAME}.changed_cartridges.txt";
  sh "${infraDir}/scripts/epages/pr/get_changed_cartridges.sh -d ${pwd()} -l ${changedCartridgesFile}"
  return readFile(changedCartridgesFile).replaceAll("\r?\n", " ")
}

/**
 * Are the changes for multistore or not. Helper method.
 *
 * @param changedCartridges String of all changed cartridges.
 * @return {@literal true} if changed cartridges are from Multistore, {@literal false} otherwise.
 */
def boolean isMultistore(String changedCartridges) {

    return changedCartridges ==~ /.*MultiStore.*/
}

/**
 * Are the changes for templates or not. Helper method.
 *
 * @param changedFiles String of all changed files.
 * @return {@literal true} if changed files contain HTML files, {@literal false} otherwise.
 */
def boolean changedHtmlFiles(String changedFiles) {

    return changedFiles ==~ /.*html/
}

/**
 * Generate String of all additional cartridges. Helper method.
 *
 * @param changedFiles String of all changed files.
 * @return String with all additional cartridges.
 */
def String additionalCartridges(String changedFiles) {

  String additionalCartridges = "";
  for (String changedFile : changedFiles.split(" ")) {
    // if(changedFile =~ (/ELogistics/|/Envialia/|/TagCommander/|/Diagnostics/)) {
    if (changedFile =~ /ELogistics/ || changedFile =~ /Envialia/ || changedFile =~ /TagCommander/ || changedFile =~ /Diagnostics/) {
      additionalCartridges += changedFile + " "
    }
  }
  return additionalCartridges
}

/**
 * Generate String of all changed perl files. Helper method.
 *
 * @param changedFiles String of all changed files.
 * @return String with all changed perl files.
 */
def String changedPerlFiles(String changedFiles) {

  String changedPerls = "" 
  for (String changedFile : changedFiles.split(" ")) {
    // if (changedFile =~ (/\.pm$/|/\.pl$/|/\.t$/)) {
    if (changedFile =~ /\.pm$/ || changedFile =~ /\.pl$/ || changedFile =~ /\.t$/) {
      changedPerls += changedFile + "\n"
    }
  }
  return changedPerls
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

/**
 * Try to run the given shell command and return the exit code, Helper method.
 *
 * @param shellCommand shell command to get return code.
 */
def int trySh(shellCommand) {

  try {
    sh([script: shellCommand])
    return 0
  } catch (e) {
    return getExitCode(e.message)
  }
}

/**
 * Get a random chuck norris joke for success mails. Helper method.
 */
def String getRandomChuckJoke() {

  def chuckAPI = "http://api.icndb.com/jokes/random"
  def chuckJSON = new JsonSlurper().parseText( chuckAPI.toURL().openConnection().content.text )
  def chuckJoke = chuckJSON.value.joke
  return chuckJoke;
}

/**
 * We don't want to run every test from Cartridge 'Test' as this consume to much time.
 * Reference: https://epages.atlassian.net/browse/AD-5049
 */
def runTestCartridgeTests(String store, String rootPath) {

  // We need to set the store first, then start to test.
  sh "export EPAGES_TESTSITE=${store} && cd ${rootPath}/Cartridges/DE_EPAGES &&  ${rootPath}/Perl/bin/perl Test/t/translationFiles.t"
  sh "export EPAGES_TESTSITE=${store} && cd ${rootPath}/Cartridges/DE_EPAGES &&  ${rootPath}/Perl/bin/perl Test/t/translationJS.t"
  sh "export EPAGES_TESTSITE=${store} && cd ${rootPath}/Cartridges/DE_EPAGES &&  ${rootPath}/Perl/bin/perl Test/t/pageTypes.t"
}

/**
 * Returns the Fully Qualified Domain Name for the node in which this function is being invoked.
 */
def getCurrentHostname() {

  sh "hostname -f | tr '\n' -d | sed -r 's/-\$//'  > /tmp/hostname.txt"
  return readFile("/tmp/hostname.txt")
}

/**
 * Send mail to committer, if a stage has errors.
 * Reference: https://epages.atlassian.net/browse/AD-4946
 * 
 * @param stage The name of the stage.
 * @param prId The id of the pull request.
 */
def sendMailToCommitter(String stage) {

  withCredentials([[
    $class: 'StringBinding', 
    credentialsId: '0204e46d-eb6f-481d-be54-ad1ed103252b',
    variable: 'jiraToken']]) {
        sh "${infraDir}/scripts/epages/github/get_infos_from_git_API.sh -p ${prId} -t ${env.jiraToken}"
  }

  String mailAdress = readFile("/tmp/pr_test_mail_adress.txt")
  String prTitle = readFile("/tmp/${prId}.title.txt")

  // https://www.cloudbees.com/blog/mail-step-jenkins-workflow
  prTitle = prTitle.replace("\n", "");
  mail (to: "$mailAdress",
        subject: "[PR Test] Your Pull request ${prTitle} failed on ${stage}.",
        body: "Your Pull Request\n${prTitle}\nfailed.\nPlease go to ${env.BUILD_URL} and check the results.\n\nThanks,\nTeam Orange");
  echo "Abort now. Mail was send to ${mailAdress}"
}

// Important for the Pipeline Remote Loader Plugin. Avoid null pointer exception.
return this