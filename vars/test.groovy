#!/usr/bin/env groovy

/**
 * Compare language files.
 */
def compareLanguageFiles() {

  stage("Compare language files") {
    sh """
      ~/epages-infra/scripts/epages/test/run_compare_language_files.sh -a
    """
  }
}

/**
 * Check for duplicated language tags.
 */
def checkDuplicatedLanguageTags() {

  stage("Check for duplicated language tags") {
    sh """
      . /etc/default/epages6
      \$PERL \$EPAGES_CARTRIDGES/DE_EPAGES/Presentation/Scripts/checkDuplicateTagDefs.pl \$EPAGES_CARTRIDGES/DE_EPAGES
    """
  }
}

/**
 * Object reference test.
 */
def checkObjectReferenceTest() {

  stage("Object reference test") {
    sh """
      . /etc/default/epages6
      \$PERL \$EPAGES_CARTRIDGES/DE_EPAGES/Test/t/ObjectReferenceTest.t
    """
  }
}

/**
 * Language Test.
 */
def checkLanguageTest() {

  stage("Language Test") {
    sh """
      # Start language tests
      for LANGUAGE in ca cs da es fr fi it nl no pt ru sv ; do
        ~/epages-infra/scripts/epages/test/run_check_language_tags.sh -s "Store" -l "\${LANGUAGE}"
        if [[ \$? -ne 0 ]] ; then ERRORS='true' ; fi
      done

      # Start tests for dialects
      for LANGUAGE in en-US pt-BR ; do
        ~/epages-infra/scripts/epages/test/run_check_language_tags.sh -s "Store\${LANGUAGE/-/}" -l "\${LANGUAGE%%-*}"
        if [[ \$? -ne 0 ]] ; then ERRORS='true' ; fi
      done

      # Exit, if errors occured
      if [[ "\${ERRORS}" = 'true' ]] ; then exit 1 ; fi
    """
  }
}

/**
 * Check language tags.
 *
 * @param Store where we want to work on.
 */
def checkLanguageTags(String store) {

  stage("Check language tags") {
    sh """
      ~/epages-infra/scripts/epages/test/run_check_language_tags.sh -s "\${store}" -l "de"
      if [[ \$? -ne 0 ]] ; then ERRORS='true' ; fi

      # Exit, if errors occured
      if [[ "\${ERRORS}" = 'true' ]] ; then exit 1 ; fi
    """
  }
}

/**
 * Run Perl module tests.
 *
 * @param Store where we want to work on.
 */
def testPerl(String store) {

  stage("Test perl module tests") {
    sh """
      ~/epages-infra/scripts/epages/test/run_perl_module_tests.sh ${store}
      if [[ \$? -ne 0 ]] ; then ERRORS='true' ; fi

      # Exit, if errors occured
      if [[ "\${ERRORS}" = 'true' ]] ; then exit 1 ; fi
    """
  }
}

/**
 * Test short URLs.
 */
def testShortUrls() {

  stage("Test short URLs") {
    sh """
      export BUILD_ID=dontKillMe
      ~/epages-infra/scripts/epages/test/test_shorturls.sh
    """
  }
}

/**
 * Test regression.
 */
def regressionTests() {

  stage("Test regression") {
    sh """
      ~/epages-infra/scripts/epages/test/run_regression_tests.sh
      if [[ \$? -ne 0 ]] ; then ERRORS='true' ; fi

      # Exit, if errors occured
      if [[ "\${ERRORS}" = 'true' ]] ; then exit 1 ; fi
    """
  }
}

/**
 * Test compile templates.
 */
def compileTemplatesTest() {

  stage("Test compile templates") {
    sh """
      ~/epages-infra/scripts/epages/compile_templates.sh
      if [[ \$? -ne 0 ]] ; then ERRORS='true' ; fi

      # Exit, if errors occured
      if [[ "\${ERRORS}" = 'true' ]] ; then exit 1 ; fi
    """
  }
}

/**
 * Test check unused files.
 */
def checkUnusedFiles() {

  stage("Test check unused files") {
    sh """
      ~/epages-infra/scripts/epages/test/run_check_for_unused_files.sh
      if [[ \$? -ne 0 ]] ; then ERRORS='true' ; fi

      # Exit, if errors occured
      if [[ "\${ERRORS}" = 'true' ]] ; then exit 1 ; fi
    """
  }
}

/**
 * Test create shop per shoptype.
 */
def createShopPerShoptype() {

  stage("Test create shop per shoptype") {
    sh """
      ~/epages-infra/scripts/epages/test/run_create_one_shop_per_shoptype_test.sh
      if [[ \$? -ne 0 ]] ; then ERRORS='true' ; fi

      # Exit, if errors occured
      if [[ "\${ERRORS}" = 'true' ]] ; then exit 1 ; fi
    """
  }
}

/**
 * Test run compile test perl files.
 */
def runCompileTestPerlFilesTest() {

  stage("Test run compile test perl files") {
    sh """
      ~/epages-infra/scripts/epages/test/run-compile-test-perl-files.sh
      if [[ \$? -ne 0 ]] ; then ERRORS='true' ; fi

      # Exit, if errors occured
      if [[ "\${ERRORS}" = 'true' ]] ; then exit 1 ; fi
    """
  }
}

/**
 * Test soap web services.
 *
 * @param Branch where we want to work on e.g. 'dev'.
 */
def testSoap(String branch) {

  stage("Test soap web services") {
    if (branch != "") {
      branch = "-b $branch"
    }

    sh """
      ~/epages-infra/scripts/epages/test/run_web_service_tests.sh -d "`hostname --fqdn`" ${branch}
      if [[ \$? -ne 0 ]] ; then ERRORS='true' ; fi

      # Exit, if errors occured
      if [[ "\${ERRORS}" = 'true' ]] ; then exit 1 ; fi
    """
  }
}

/**
 * Test sonar.
 */
def testSonar() {

  stage("Test sonar") {
    sh """
      export BUILD_ID=dontKillMe
      set +se
      . /etc/default/epages6
      cd \$EPAGES_CARTRIDGES/DE_EPAGES
      make sonar_analysis SONAR_SERVER_URL=http://easy.vm-intern.epages.com:9000 || true
    """
  }
}

/**
 * Test perl critic.
 */
def testCritic() {

  stage("Test perl critic") {
    sh """
      export BUILD_ID=dontKillMe
      ~/epages-infra/scripts/epages/test/run_perl_critic_test.sh -b 'bin'
    """
  }
}
