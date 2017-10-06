#!/bin/bash -e

BN=${0##*/}

#-----------------------------------------------------------------------------
# get GIT repo
#-----------------------------------------------------------------------------

( if [[ -d ~jenkins/jenkins-rnd-pipeline/.git ]] ; then
  cd ~jenkins/jenkins-rnd-pipeline
  git checkout dev
  git pull git@github.com:ePages-de/jenkins-rnd-pipeline dev
else
  cd ~jenkins
  git clone git@github.com:ePages-de/jenkins-rnd-pipeline
  cd ~jenkins/jenkins-rnd-pipeline
fi
) >/dev/null 2>&1

cd ~jenkins/jenkins-rnd-pipeline/public-keys

#-----------------------------------------------------------------------------
# arguments
#-----------------------------------------------------------------------------

usage() {
  [[ -z $1 ]] || echo -e "Error:\n  $1"
  echo 'Usage:'
  echo "  $BN [-n 'NAME_GLOB ...' ] [-k 'KEY_GLOB ...']"
  echo 'Options:'
  echo '  -n: show names of public keys for glob list'
  echo '  -k: show public keys for glob list'
  exit 1
}

unset SHOW_NAMES SHOW_KEYS
while getopts n:k: c ; do
  case $c in
    n) SHOW_NAMES="$OPTARG" ;;
    k) SHOW_KEYS="$OPTARG" ;;
    *) usage ;;
  esac
done
shift $(expr $OPTIND - 1)
[[ $# -eq 0 ]] || usage

#-----------------------------------------------------------------------------
# SHOW_NAMES xor SHOW_KEYS
#-----------------------------------------------------------------------------

if [[ -n $SHOW_NAMES ]] ; then
  if echo "$SHOW_NAMES" | tr -d '[A-Za-z0-9_.@ *?{,}[]\-]' | grep . ; then
    usage "void chars in SHOW_NAMES"
  fi
  ls -1d $SHOW_NAMES 2>/dev/null || true
elif [[ -n $SHOW_KEYS ]] ; then
  if echo "$SHOW_KEYS" | tr -d '[A-Za-z0-9_.@ *?{,}[]\-]' | grep . ; then
    usage "void chars in SHOW_KEYS"
  fi
  cat $SHOW_KEYS 2>/dev/null || true
else
  usage "both SHOW_NAMES and SHOW_KEYS are empty"
fi

exit 0
