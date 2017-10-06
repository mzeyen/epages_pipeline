#!/bin/bash -e

BN=${0##*/}

#-----------------------------------------------------------------------------
# get GIT repo
#-----------------------------------------------------------------------------

if [[ -d ~jenkins/jenkins-rnd-pipeline/.git ]] ; then
  cd ~jenkins/jenkins-rnd-pipeline
  git checkout dev
  git pull git@github.com:ePages-de/jenkins-rnd-pipeline dev
else
  cd ~jenkins
  git clone git@github.com:ePages-de/jenkins-rnd-pipeline
  cd ~jenkins/jenkins-rnd-pipeline
fi

mkdir -p ~jenkins/jenkins-rnd-pipeline/public-keys

#-----------------------------------------------------------------------------
# arguments
#-----------------------------------------------------------------------------

usage() {
  [[ -z $1 ]] || echo -e "Error:\n  $1"
  echo 'Usage:'
  echo "  $BN [-a ACCOUNT] -p PUBLIC_KEY -A 'AUTHOR <EMAIL>'"
  echo 'Options:'
  echo '  -a: either e-mail or host account (must contain @)'
  echo '  -p: public key, must start with ssh-rsa or so'
  exit 1
}

unset PUBLIC_KEY ACCOUNT AUTHOR
while getopts p:a:A: c ; do
  case $c in
    p) PUBLIC_KEY=$OPTARG ;;
    a) ACCOUNT=$OPTARG ;;
    A) AUTHOR=$OPTARG ;;
    *) usage ;;
  esac
done
shift $(expr $OPTIND - 1)
[[ $# -eq 0 ]] || usage

#-----------------------------------------------------------------------------
# check AUTHOR
#-----------------------------------------------------------------------------

[[ -n $AUTHOR ]] || usage "variable missing: AUTHOR"

#-----------------------------------------------------------------------------
# check PUBLIC_KEY
#-----------------------------------------------------------------------------

[[ -n $PUBLIC_KEY ]] || usage "variable missing: PUBLIC_KEY"
if [[ -n ${PUBLIC_KEY//[A-Za-z0-9_.@\/=+ ,-]/} ]] ; then
  usage "void chars in PUBLIC_KEY: ${PUBLIC_KEY//[A-Za-z0-9_.@\/=+ ,-]/}"
fi

case "$PUBLIC_KEY" in
  ssh-rsa\ *|ssh-dss\ *|ssh-ed25519\ *) true ;;
  ecdsa-sha2-nistp256\ *|ecdsa-sha2-nistp384\ *|ecdsa-sha2-nistp521\ *) true ;;
  *) usage "PUBLIC_KEY does not start with ssh-rsa or so" ;;
esac

#-----------------------------------------------------------------------------
# get ACCOUNT from PUBLIC_KEY
#-----------------------------------------------------------------------------

if [[ -z $ACCOUNT ]] ; then
  SKIP_PARTS=2 ; COUNT=0
  while read part ; do
    COUNT=$((COUNT + 1))
    [[ $COUNT -lt $SKIP_PARTS ]] && continue
    [[ $COUNT = $SKIP_PARTS ]] && { ACCOUNT=$part; continue; }
    [[ $part =~ @ ]] && ACCOUNT=$part
  done < <(echo "$PUBLIC_KEY" | tr ' ' '\n')
fi

#-----------------------------------------------------------------------------
# check ACCOUNT
#-----------------------------------------------------------------------------

ACCOUNT=${ACCOUNT//,/}

[[ -n $ACCOUNT ]] || usage "variable missing: ACCOUNT"
[[ $ACCOUNT = ${ACCOUNT#[-@]} ]] || usage "ACCOUNT must not start with '-' or '@'"
[[ $ACCOUNT = ${ACCOUNT%[-@]} ]] || usage "ACCOUNT must not end with '-' or '@'"
[[ $ACCOUNT != ${ACCOUNT#*@} ]] || usage "ACCOUNT must contain '@'"
[[ -z ${ACCOUNT//[A-Za-z0-9_.@-]/} ]] || usage "void chars in ACCOUNT: ${ACCOUNT//[A-Za-z0-9_.@-]/}"

#-----------------------------------------------------------------------------
# update GIT repo
#-----------------------------------------------------------------------------

BRANCH="public-key/$ACCOUNT-$(date +%Y-%m-%d)"
COMMIT_MESSAGE="update public key for $ACCOUNT"

git checkout -b "$BRANCH"

echo "$PUBLIC_KEY" > "public-keys/$ACCOUNT"
git add public-keys
git commit -a -m "$COMMIT_MESSAGE" --author "$AUTHOR" || true

git checkout dev
git merge "$BRANCH"

git push git@github.com:ePages-de/jenkins-rnd-pipeline dev

git branch -D "$BRANCH"

exit 0
