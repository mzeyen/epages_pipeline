#!/usr/bin/env groovy

/**
 * Configure application server.
 *
 * @param memoryAmount the amount of memory for one application server.
 * @param numberServers the number of application servers.
 */
def configureAppServers(String memoryAmount, String numberServers ) {

  stage("Configure app server") {
    sh """
      export BUILD_ID=dontKillMe
      ~/epages-infra/scripts/epages/configure_app_servers.sh -m ${memoryAmount} -n {$numberServers} -r
    """
  }
}

/**
 * Stop epages cron jobs.
 */
def stopCronJobs() {

  stage("Stop cron jobs") {
    sh """
      /etc/init.d/epages6 stop_cron
    """
  }
}

/**
 * Stop epages.
 */
def stopEpages() {

  stage("Stop ePages") {
    sh """
      /etc/init.d/epages6 stop
    """
  }
}

/**
 * Source ePages.
 */
def sourceEp() {

  stage("Source ePages") { }
}

/**
 * Update host file.
 *
 * @param version The version for patching.
 * @param alphaVm The alpa VM of a distributed installation (Fqdn)
 * @param databaseVm The databas VM of a distributed installation (Fqdn)
 * @param webServerVm The webserver VM of a distributed installation
 */
def updateHostFile(String version,
                   String alphaVm,
                   String databaseVm,
                   String webServerVm) {

  stage("Update host file") {
    sh """
      ~/epages-infra/scripts/epages/di/di_patch_update_hosts_file_asj.sh -v ${version} -a ${alphaVm} -d ${databaseVm} -w ${webServerVm}
    """
  }
}

/**
 * Use ePages iniConfig.
 */
def iniConfig() {

  stage("iniConfig") { }
}

/**
 * Start ePages services.
 */
def startEpService() {

  stage("Start ePages services") { }
}

/**
 * Write a shop config.
 */
def writeShopConfig() {

  stage("Write shop config") { }
}

/**
 * Write a shop config.
 */
def writeStoreConf() {

  stage("Write Store conf") { }
}

/**
 * Add cartridge to business unit.
 *
 * @param store the store you want to add the cartridge.
 * @param cartridge the cartridge itself in "DE_EPAGES::Cartridge" syntax.
 */
def addCartridgesToBu(String store, String cartridge) {

  stage("Add Cartridge to BU") {
    sh """
      export BUILD_ID=dontKillMe
      /var/epages/bin/add-cartridges-to-bu.sh -s ${store} -a ${cartridge}
    """
  }
}

/**
 * Update mirrors.
 */
def updateMirrors() {

  stage("Update Mirrors") {
    sh """
      # Copy files from ~/epages-infra/configuration/yum.repos.d to /etc/yum.repos.d
      ~/epages-infra/scripts/centos/update-centos-mirrors.sh
    """
  }
}

/**
 * Configure Store on Site.
 */
def configureStoreOnSite() {

  stage("Configure Store on Site") { }
}

/**
 * Configure dialects.
 */
def dialects() {

  stage("Configure dialects") {
    sh """
      export BUILD_ID=dontKillMe
      . /etc/default/epages6
      cd \$EPAGES_CARTRIDGES/DE_EPAGES
      \$PERL Makefile.PL

      /bin/cp -p \$EPAGES_STORES/Site/ShopImport/LocaleDemo.xml .
      for dialect in ptBR enUS ; do
        cat ~/epages-infra/configuration/install-epages/XML/LocaleDemo-\$dialect.xml > \$EPAGES_STORES/Site/ShopImport/LocaleDemo.xml
        make install_demoshop STORE=Store\$dialect DEMOSHOP_ALIAS=Store\${dialect}_Demo
      done
      /bin/mv LocaleDemo.xml \$EPAGES_STORES/Site/ShopImport
    """
  }
}

/**
 * Setup Unity SSL.
 */
def setupUnitySsl() {

  stage("Setup Unity SSL") {
    sh """
      export BUILD_ID=dontKillMe

      # copy certificates
      curl http://am-vm-intranet.intern.epages.de/AM/certificates/_.intern.epages.de/_.intern.epages.de.crt | tee /etc/apache2-epages/ssl.crt/unity.crt > /etc/apache2-epages/ssl.crt/server.crt
      curl http://am-vm-intranet.intern.epages.de/AM/certificates/_.intern.epages.de/_.intern.epages.de.key | tee /etc/apache2-epages/ssl.key/unity.key > /etc/apache2-epages/ssl.key/server.key
      curl http://am-vm-intranet.intern.epages.de/AM/certificates/_.intern.epages.de/ca.crt > /etc/apache2-epages/ssl.crt/ca.crt

      # Change /etc/default/epages6
      sed -i "s/^HTTPD_OPTS=.*/HTTPD_OPTS='-DSSL -DVHOSTS_NAME -DUNITY_VHOST'/" /etc/default/epages6

      # Set Unity connection parameter (some can be set in TBO manually)
      . /etc/default/epages6
      \$PERL \$EPAGES_CARTRIDGES/DE_EPAGES/Object/Scripts/set.pl -storename Site -path "/Stores/Store" UnityUrlScheme="\$(hostname --fqdn)"
      \$PERL \$EPAGES_CARTRIDGES/DE_EPAGES/Object/Scripts/set.pl -storename Site -path "/Stores/Store" UnityApiUrlScheme="api.\$(hostname --fqdn)/api/v2"
      \$PERL \$EPAGES_CARTRIDGES/DE_EPAGES/Object/Scripts/set.pl -storename Site -path "/Stores/Store" UnityUseSubdomainRouting="1"
      \$PERL \$EPAGES_CARTRIDGES/DE_EPAGES/Object/Scripts/set.pl -storename Site -path "/Stores/Store" HasSSLCertificateUnity="1"
      \$PERL \$EPAGES_CARTRIDGES/DE_EPAGES/Object/Scripts/set.pl -storename Site -path "/Stores/Store" UnityWebServerPort="80"
      \$PERL \$EPAGES_CARTRIDGES/DE_EPAGES/Object/Scripts/set.pl -storename Site -path "/Stores/Store" UnitySSLWebServerPort="443"
      \$PERL \$EPAGES_CARTRIDGES/DE_EPAGES/Object/Scripts/set.pl -storename Store -path / HasSSLCertificate="1"  UnityBaseURL="http://\$(hostname --fqdn)"

      # Set set jwtsecret
      /var/epages/ini-config.sh -i -f \$EPAGES_CONFIG/Database.d/Store.conf -k jwtsecret -v 'this-is-super-secret---not'

      service epages6 start
      service epages6 start_java
    """
  }
}

/**
 * Create Unity shops.
 */
def createUnityShops() {

  stage("Create Unity Shops") {
    sh """#!/bin/bash
      export BUILD_ID=dontKillMe
      . /etc/default/epages6

      CREATE_SHOP=/srv/epages/eproot/Cartridges/DE_EPAGES/ShopConfiguration/Scripts/createShop.pl
      SET_PL=/srv/epages/eproot/Cartridges/DE_EPAGES/Object/Scripts/set.pl
      for shop in testshopunity esf-test ; do
        echo \$PERL \$CREATE_SHOP -wsuser Providers/Distributor/Users/admin -passwd admin -shoptype UnityDE -storename Store -alias \$shop -scriptname \$shop
        \$PERL \$CREATE_SHOP -wsuser Providers/Distributor/Users/admin -passwd admin -shoptype UnityDE -storename Store -alias \$shop -scriptname \$shop || exit 1
        echo \$PERL \$SET_PL -storename Store -path /Shops/\$shop MailFrom=m.zeyen@epages.com
        \$PERL \$SET_PL -storename Store -path /Shops/\$shop MailFrom=m.zeyen@epages.com
      done
    """
  }
}

/**
 * Update config files with FLEX_HOSTS.
 *
 * @param Repo url e.g. ftp://ftp.epages.com/pub/epages/build/repo/${version}/latest
 * @param VM we want to work on.
 */
def updateConf(String repo, String flexStoreVm) {

  stage("Write FlexStore VM to config file.") {
    sh """
      export BUILD_ID=dontKillMe
      ansible-playbook /var/epages/flex/ansible/update-conf.yml -e repo=${repo} -e \"{'stores': ${flexStoreVm}}\"
    """
  }
}

/**
 * Copy the public key to all Flex stores.
 *
 */
def copyPublicKeysToStore() {

  stage("Copy public ssh key to FlexStore VM") {
    sh """
      export BUILD_ID=dontKillMe
      for host in ${FLEX_HOSTS} ; do
      ssh-copy-id ${host}
      done
    """
  }
}

/**
 * Check Flex store connections.
 *
 */
def checkFlexStoreConnection() {

  stage("Check connection to FlexStore VM") {
    sh """
      export BUILD_ID=dontKillMe
      ansible flexstores -a hostname
    """
  }
}

/**
 * Install SSl proxy.
 *
 */
def installSslProxy() {

  stage("Install SSl proxy") {
    sh """
      export BUILD_ID=dontKillMe
      ~/epages-infra/scripts/epages/install_sslproxy.sh
    """
  }
}

/**
 * Run scheduler on Monitor BU.
 *
 * @param The Store where we want to work on.
 */
def runSchedulerOnStore(String storeName) {

  stage("Run scheduler on Monitor BU") {
    sh """#!/bin/bash
      . /etc/default/epages6
      \$PERL \$EPAGES_CARTRIDGES/DE_EPAGES/Database/Scripts/runSchedulerOnStore.pl -storename ${storeName} -active -run
    """
  }
}

/**
 * Import file.
 *
 * @param Path to the object e.g. "/Providers/Distributor".
 * @param Path to the file we want to import e.g. "/root/epages-infra/configuration/nightlies/DeveloperShopType.xml".
 */
def importPl(String path, String filePath) {

  stage("Import file $filePath") {
    sh """#!/bin/bash
      . /etc/default/epages6
      \$PERL \$EPAGES_CARTRIDGES/DE_EPAGES/Object/Scripts/import.pl -storename Site -path ${path} ${filePath}
    """
  }
}

/**
 * Enable SSL.
 *
 * @param The Store where we want to work on.
 */
def enableSSL(String storeName) {

  stage("Enable SSL on $storeName") {
    sh """
      export BUILD_ID=dontKillMe
      /root/epages-infra/scripts/epages/util/enable_ssl.sh -s ${storeName}
    """
  }
}

/**
 * Create Shop.
 *
 * @param Name of Shop
 * @param Type of Shop
 */
def createShop(String shopName, String shopType) {

  stage("Create Shop $shopName") {
    sh """
      export BUILD_ID=dontKillMe
      ~/epages-infra/scripts/epages/create_shop.sh -n ${shopName} -t ${shopType} -s Store -i
    """
  }
}

/**
 * Create Shops.
 *
 * @param A map of shops we want to create.
 */
def createShops(Map shops) {

  stage("Create shops") {
    keys = shops.keySet() as String[]
    for(int i = 0; i < keys.size(); i++) {

      def shopName = keys[i]
      def shopType = shops.get(keys[i])
      sh """
        export BUILD_ID=dontKillMe
        ~/epages-infra/scripts/epages/create_shop.sh -n ${shopName} -t ${shopType} -s Store -i
      """
    }
  }
}

/**
 * Set pl.
 *
 * @param The store where we want to work on e.g. "Site".
 * @param Path to the object e.g. "/".
 * @param The object value we want to change e.g "SMTPServer=localhost".
 */
def setPl(String storeName, String path, String value) {

  stage("setPl") {
    sh """#!/bin/bash
      export BUILD_ID=dontKillMe
      . /etc/default/epages6
      \$PERL \$EPAGES_CARTRIDGES/DE_EPAGES/Object/Scripts/set.pl -storename ${storeName} -path ${path} ${value}
    """
  }
}

/**
 * Configure solr.
 *
 * reduce autocommit maxTime to 1000 just for rest api tests.
 */
def solrAutoCommitMaxTime() {

  stage("Configure solr autocommit") {
    sh """
      export BUILD_ID=dontKillMe
      sed -ri 's,(<maxTime>)(.*)(</maxTime>),\11000\3,' /srv/epages/eproot/j/solr/conf/solrconfig.xml
      service epages-solr restart || true
    """
  }
}

/**
 * Update Search Index.
 */
def updateSearchIndex() {

  stage("Update search index") {
    sh """
      export BUILD_ID=dontKillMe
      ~/epages-infra/scripts/epages/update_search_index.sh -s Store
    """
  }
}

/**
 * Deploy epages cartrigdes on the test VM.
 */
def getCartridges() {

  stage("Deploy ePages cartrigdes") {
    sh """
      if [ ! -d ~/Cartridges/.git ] ; then
        cd ~
        git clone git@github.com:ePages-de/Cartridges.git
      else
        cd ~/Cartridges && git pull origin develop
      fi
    """
  }
}

/**
 * Install SSL certificates.
 */
def getSslCertificates() {

  stage("Installing ssl certificates.") {
    sh """
      cp /etc/certificates/ssl.crt/ca.crt /etc/apache2-epages/ssl.crt/ca.crt
      cp /etc/certificates/ssl.crt/server.crt /etc/apache2-epages/ssl.crt/server.crt
      cp /etc/certificates/ssl.key/server.key /etc/apache2-epages/ssl.key/server.key
      echo 'SSLProtocol -all +TLSv1.2' >> /etc/apache2-epages/httpd.conf
      service epages6 start_httpd reload
    """
  }
}

/**
 * Prepare taxus tests.
 */
def prepareTaxUsTest() {

  stage("Prepare taxus tests.") {
    sh """
      export BUILD_ID=dontKillMe
      ~/epages-infra/scripts/esf/prepare-taxus-tests.sh -s Store
    """
  }
}

/**
 * Activate logging for given cartridges.
 *
 *@param list of cartridges where we want to log.
 */
def activateLogging(cartridge_list) {

  stage("Activate logging for given cartridges.") {
    for (String cartridge : cartridge_list) {
      sh """
        ~/epages-infra/scripts/epages/util/activate-logging.sh -p "${cartridge}" -s 'Store'
      """
    }
  }
}

/**
 * Apply security changes.
 */
def applySecurityChanges() {

  stage("Apply security changes to epages installation") {
    sh "~/epages-infra/scripts/epages/security/make_security_changes.sh"
  }
}

/**
 * Set correct time on VM.
 */
def setTime(String envOs) {

  stage("Set correct time for VM.") {
    if (envOs == "debian") {
      sh """
        apt-get update
        apt-get install -y ntpdate
        ntpdate -u pool.ntp.org || true
      """
    } else {
      sh """
        yum -y install ntpdate
        ntpdate -u pool.ntp.org || true
      """
    }
  }
}

/**
 * Copy checked out DE_EPAGES to $EPAGES_CARTRIDGES
 */
def copyCartridgesToEpagesCartridges() {

  stage("Copy checked out DE_EPAGES to EPAGES_CARTRIDGES") {
    sh """
      . /etc/default/epages6
      cd /root/Cartridges
      rm -Rf .git
      tar cf - CPAN DE_EPAGES Cartridges.xml | tar -C \$EPAGES_CARTRIDGES -xf -
    """
  }
}

/**
 * Get latest stable build number from specified job.
 *@param jobName Job where to get build number from.
 */
def getLatestStableBuildId(String jobName) {
    def job = Jenkins.instance.getItemByFullName(jobName)
    def latestBuild = job.getLastStableBuild()
    String latestBuildId = latestBuild.toString().minus(jobName + ' #')
    return latestBuildId
}

/**
 * Create test shops for ZDT patch.
 */
def createZdtTestShops() {

    stage("Create test shops for ZTD") {
        sh "~/epages-infra/scripts/epages/setup_zdt_installation.sh"
    }
}

/**
 * Backup Cartridges folder.
 */
def backupCartridgesFolder() {

    stage("Backup Cartridges folder") {
        sh "mv /srv/epages/eproot/Cartridges /srv/epages/eproot/Cartridges_orig"
    }
}
