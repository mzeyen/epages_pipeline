#!/bin/bash
echo ">>>>>>>>>>>>>>>>>>>>>>>>>>"
echo "CLEANING UP..."

sudo docker images -q --filter "dangling=true" | xargs -r sudo docker rmi
sudo docker rmi $(sudo docker images -q epages/esf)

if [[ -d esf ]] ; then sudo rm -Rf esf ; fi

mkdir esf
mkdir esf/log

echo "CLEAN UP DONE."
echo "<<<<<<<<<<<<<<<<<<<<<<<<<<"

echo ">>>>>>>>>>>>>>>>>>>>>>>>>>"
echo "GETTING IMAGES..."

#wget "http://jenkins.intern.epages.de:8080/job/Build_ESF/ws/buildesf-latest.tar" > "esf/buildesf-latest.tar"
set +x ; docker login -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD} ; set -x
docker pull epages/esf:pre-release-development
#mv buildesf-latest.tar esf/buildesf-latest.tar
#sudo docker load < esf/buildesf-latest.tar

echo "IMAGES ARE LOADED."
echo "<<<<<<<<<<<<<<<<<<<<<<<<<<"

export DISPLAY=":0"

echo ">>>>>>>>>>>>>>>>>>>>>>>>>>"
echo "SETTING ESF PARAMETERS..."

# This part needs a refactoring.
if [[ ${groups_to_test} = 'eCMS' ]] ; then 
  ESF_PARAMETERS="-browser $browser -groups $groups_to_test -shop eCMSFree -url http://${TARGET_HOST}/epages"
else
  ESF_PARAMETERS="-browser $browser -groups $groups_to_test -shop esf-int-$operating_system-$browser-$groups_to_test -url http://${TARGET_HOST}/epages"
fi
  
# The USTAX and ERPShop are without a shop domain.
if [[ ${groups_to_test} != 'USTAX' && ${groups_to_test} != 'ERPShop' && ${groups_to_test} != 'CORE' && ${groups_to_test} != 'SEARCH' && ${groups_to_test} != 'eCMS' ]] ; then 
  # Lets check, if we work on schwarzbart.epages.systems
  if [[ ${TARGET_HOST} == *"schwarzbart.epages.systems" ]] ; then
    ESF_PARAMETERS="${ESF_PARAMETERS} --shop-domain esf-int-$operating_system-$browser-$groups_to_test.epages.systems"
  else
    ESF_PARAMETERS="${ESF_PARAMETERS} --shop-domain esf-int-$operating_system-$browser-$groups_to_test.epages.com"
  fi
fi

# We cannot use the performance test for the ERP test shop.
if [[ ${groups_to_test} = 'ERPShop' || ${groups_to_test} = 'CORE' || ${groups_to_test} = 'SEARCH' || ${groups_to_test} = 'eCMS' ]] ; then 
  ESF_PARAMETERS="${ESF_PARAMETERS} -ap 0 -sp --restart-browser"
fi

echo "ESF PARAMETERS GENERATED: ${ESF_PARAMETERS}"
echo "<<<<<<<<<<<<<<<<<<<<<<<<<<"

echo ">>>>>>>>>>>>>>>>>>>>>>>>>>"
echo "RUNNING ESF TESTS..."

sudo docker run --rm --user=root --cap-add=SYS_ADMIN -v ${WORKSPACE}/esf/log:/home/esf/esf/log -t epages/esf:pre-release-development ${ESF_PARAMETERS}
EXIT_CODE_ESF="$?"

echo "ESF TESTS DONE."
echo "<<<<<<<<<<<<<<<<<<<<<<<<<<"

echo ">>>>>>>>>>>>>>>>>>>>>>>>>>"
echo "PREPARING LOGSTASH"

for RESULT_FILE in `find "${WORKSPACE}/esf/log" -name "emailable-report.html" -type f | sort | tail -n1` ; do cp -vf "$RESULT_FILE" /tmp/. ; done

EXIT_CODE_LOGSTASH=0

# push the esf-test-results.json to our elasticsearch server via logstash docker container.

############
# logstash #
############

# mount dirs.
export LS_LOG="$(find ${WORKSPACE} -maxdepth 3 -name "log" -type d)"
export LS_CONFIG="${WORKSPACE}/to-logstash/config"

# logstash.conf.
export LS_INPUT="log,esf"
export LS_OUTPUT="log,elasticsearch"

###########
# esf env #
###########

# epages6.
export EPAGES_VERSION=${VERSION}
export EPAGES_REPO_ID=${REPO}

# env url to dir ".../esf/.../log".
export ENV_URL="${BUILD_URL}artifact/esf/${LS_LOG#*/esf/}"

#################
# elasticsearch #
#################

# elasticsearch connection details.
export ES_HOSTS="[ 'cd-vm-docker-host-001.intern.epages.de:9200' ]"
export ES_USER="admin"
export ES_PASSWORD="qwert6"

# elasticsearch document path.
export ES_INDEX="esf-build-ui-tests"

##########
# docker #
##########

# docker container settings.
export LS_DOCKER_CONTAINER="to-logstash-esf-build-${BUILD_NUMBER}"ll

# pull latest logstash image.
set +x
sudo docker login -u ${DOCKER_USERNAME} -p "${DOCKER_PASSWORD}" -e teamorange@epages.com
set -x

sudo docker pull ${TO_LOGSTASH_IMAGE_NAME}:${TO_LOGSTASH_IMAGE_TAG}

# run logstash docker container.
${WORKSPACE}/to-logstash/deploy.sh || EXIT_CODE_LOGSTASH=1

# reown complete workspace.
sudo chown -R jenkins:jenkins ${WORKSPACE} || EXIT_CODE_LOGSTASH=1

# exit with saved exit code from previous script.
if [[ ${EXIT_CODE_ESF} -ne 0 || ${EXIT_CODE_LOGSTASH} -ne 0 ]] ; then exit 1 ; fi

echo "PREPARING LOGSTASH DONE."
echo "<<<<<<<<<<<<<<<<<<<<<<<<<<"
