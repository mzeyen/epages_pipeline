#!/bin/bash -xe

if [[ -d ~jenkins/jenkins-rnd-pipeline/.git ]] ; then
  cd ~jenkins/jenkins-rnd-pipeline
  git checkout dev
  git pull git@github.com:ePages-de/jenkins-rnd-pipeline dev
else
  cd ~jenkins
  git clone git@github.com:ePages-de/jenkins-rnd-pipeline
  cd ~jenkins/jenkins-rnd-pipeline
fi

BRANCH=config.xml/$(date +%Y-%m-%d)
COMMIT_MESSAGE='backup config.xml from ~/jobs into files/home/jenkins/jobs'
COMMITTER_NAME='Team Orange'
COMMITTER_EMAIL='teamorange@epages.com'

git checkout -b $BRANCH

( cd .. ; tar cf - jobs/*/config.xml ) | ( cd files/home/jenkins ; tar xf - )

git add files/home/jenkins/jobs

git commit -a -m "$COMMIT_MESSAGE" --author "$COMMITTER_NAME <$COMMITTER_EMAIL>" || true

git checkout dev
git merge $BRANCH

git push git@github.com:ePages-de/jenkins-rnd-pipeline dev

git branch -D $BRANCH

exit 0
