#!/usr/bin/env groovy

/**
 * Install additional cartridges for MultiStores.
 *
 * 1. Load git module.
 * 2. Clone epages-infra.
 * 3. Start the script for installing additional cartidges.
 */
def installAdditionalCartridgesMS() {

    stage('Install additional cartridges for MultiStore') {
        // Load git module
        git = load("jobs/modules/git.groovy")

        // Be sure you have cloned epages-infra before
        git.clone('ePages-de', 'epages-infra')

        // Install additional cartridges for MS
        sh 'epages-infra/scripts/epages/install_all_additional_cartridges.sh -m'
    }
}

/**
 * Install additional cartridges with given configuration.
 *
 * @param configuration The configuration with additional cartridges.
 */
def installAdditionalCartridges(String configuration) {

    stage("Install additional cartridges") {
        sh """
      export BUILD_ID=dontKillMe
      /var/epages/install-additional-cartridges.sh -c ${configuration}
    """
    }
}

/**
 * Install language packs.
 *
 * @param locales languages you want to translate e.g.: "fr it ru"
 */
def installLanguagePacks(String locales) {

    stage('Install language packs') {
        sh """
      # Copy config file to correct directory
      . /etc/default/epages6
      cp /var/epages/conf/locales.conf \$EPAGES_CONFIG/locales.conf

      # Install french language pack
      ~/epages-infra/scripts/epages/install_language_packs.sh -l ${locales}
    """
    }
}

/**
 * Start the Reinstall for epages6 for MultiStore.
 *
 * 1. Source /etc/default/epages6.
 * 2. Start reinstall via Makefile.PL for MultiStore.
 */
def reinstallEpagesMS() {

    stage('Reinstall epages6') {
        // Read in environment variables and start reinstall of MultiStore
        sh 'source /etc/default/epages6 &&' +
                'cd ${EPAGES_CARTRIDGES}/DE_EPAGES && ' +
                '$PERL Makefile.PL &&' +
                'make reinstall_multistore_complete'
    }
}

/**
 * Install the latest released version
 *
 * @param repo The repo for installation.
 * @installConf The install conf file you want to use for the installation.
 */
def installEpagesFromRepo(String installConf, String repo) {

    stage("Install epages, repo: ${repo}, conf: ${installConf}") {
        sh """
      export BUILD_ID=dontKillMe
      ~/epages-infra/scripts/epages/jenkins-installs-epages.sh -i ${installConf} -r ftp://ftp.epages.com/pub/epages/build/repo/${repo}
    """
    }
}

/**
 * Install the latest released version
 *
 * @param version The version for installation.
 * @param installConf the install conf file you want to use for the installation.
 */
def installEpagesFromVersion(String installConf, String version) {

    stage("Install epages, repo: ${repo}, conf: ${installConf}") {
        sh """
      export BUILD_ID=dontKillMe
      ~/epages-infra/scripts/epages/jenkins-installs-epages.sh -i ${installConf} -r ftp://ftp.epages.com/pub/epages/build/repo/${version}/latest
    """
    }
}

/**
 * Install just the epages fileserver
 *
 * @param version The version for installation.
 */
def installFileserverFromVersion(String version) {

    stage("Install Fileserver") {
        String epagesRepoUrl = "ftp://ftp.epages.com/pub/epages/build/repo/${version}/latest"
        sh """
      export BUILD_ID=dontKillMe
      export EPAGES_INSTALLATION='fileserver'
      curl -L ${epagesRepoUrl}/install-epages.sh | bash
      /bin/cp -rp ~/epages-infra/configuration/install-epages/simple /srv/epages/etc/sysconfig
    """
    }
}

/**
 * Finish install just the epages fileserver
 *
 * @param version The version for installation.
 */
def finishInstallFileserverFromVersion(String version) {

    stage("Finish install Fileserver") {
        String epagesRepoUrl = "ftp://ftp.epages.com/pub/epages/build/repo/${version}/latest"
        String configuration = "/srv/epages/etc/sysconfig/simple/install-epages-distributed.conf"
        sh """
      export BUILD_ID=dontKillMe
      /var/epages/install-epages.sh --version ${version} --repo ${epagesRepoUrl} --configfile ${configuration}
    """
    }
}

/**
 * Install Services distributed after the give configuration (install-epages-distributed.conf).
 *
 * @param version The version for installation.
 * @param fileserver The fileserver for this distributed installation.
 */
def installDistributedFromConf(String fileserver, String version) {

    stage("Install services from configuration") {
        String epagesRepoUrl = "ftp://ftp.epages.com/pub/epages/build/repo/${version}/latest"
        String configuration = "/srv/epages/etc/sysconfig/simple/install-epages-distributed.conf"
        sh """
      export BUILD_ID=dontKillMe
      export EPAGES_INSTALLATION='fileserver=${fileserver}'
      curl -L ${epagesRepoUrl}/install-epages.sh | bash
      /var/epages/install-epages.sh --version ${version} --repo ${epagesRepoUrl} --configfile ${configuration}
    """
    }
}

/**
 * Install/update epages-release
 *
 * @param version The version for installation.
 */
def installEpagesReleaseFromVersion(String version) {

    stage("Update epages-release to latest version") {
        sh """
      export BUILD_ID=dontKillMe
      export EPAGES_REPOURL=ftp://ftp.epages.com/pub/epages/build/repo/${version}/latest
      export EPAGES_INSTALLATION='-epages'
      curl -L \$EPAGES_REPOURL/install-epages.sh | bash
    """
    }
}

/**
 * Install ansible package/tool
 *
 */
def installAnsible() {

    stage("Install anisble") {
        sh """
      export BUILD_ID=dontKillMe
      yum -y install epel-release
      sed -ri 's,(enabled=).*,\\10,' /etc/yum.repos.d/epel.repo
      yum -y install ansible --enablerepo epel
    """
    }
}

/**
 * Install Flex store.
 *
 */
def installFlexStore(String flexStoreVm) {

    stage("Install Flex on FlexStore") {
        sh """
      export BUILD_ID=dontKillMe
      ansible-playbook /var/epages/flex/ansible/install.yml --limit ${flexStoreVm} -vvv
    """
    }
}

/**
 * Install epages-release from public
 *
 * @param version The version for installation.
 */
def installEpagesFromPublicRepo(String previousVersion) {

    stage("Update epages-release to latest version") {
        sh """
            export BUILD_ID=dontKillMe
            export EPAGES_REPOURL=ftp://epages-software.de/repo/usr/0914cf1e-0d62-47c8-8ca1-14e4ca3ef036
            ~/epages-infra/scripts/epages/jenkins-installs-epages.sh -i simple/install-epages-public.conf -r \$EPAGES_REPOURL -b $previousVersion
        """
    }
}

/**
 * Install latest epages version from vaariables file
 */
def installLatestEpagesFromVariables() {

    String versionString = sh(script: 'echo $(awk -F= "/^CURRENT_EPAGES_VERSION_NUMBER=/{print \$2}" ~/epages-infra/variables)', returnStdout: true)
    String version = versionString.minus("CURRENT_EPAGES_VERSION_NUMBER=").trim()

    stage("Install latest epages version from variables") {
        sh """
            ~/epages-infra/scripts/epages/jenkins-installs-epages.sh -i simple/install-epages.conf -r ftp://ftp.epages.com/pub/epages/build/repo/${version}/latest
        """
    }
}

/**
 * Start the Reinstall for epages6.
 *
 * 1. Source /etc/default/epages6.
 * 2. Start reinstall via Makefile.PL.
 */
def reinstallEpages() {

    stage('Reinstall epages6') {
        sh """
            source /etc/default/epages6 &&
            cd \${EPAGES_CARTRIDGES}/DE_EPAGES
            \$PERL Makefile.PL
            make reinstall
        """
    }
}
