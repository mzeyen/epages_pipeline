# Next generation pipelines with Jenkins

Currently in testing phase this repository is all about making use of [Jenkins' Pipeline Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Pipeline+Plugin).

## Build Status 

| `dev` | `master` | `stable` |
| :---: | :------: | :------: |
| [![Circle CI](https://circleci.com/gh/ePages-de/jenkins-rnd-pipeline/tree/dev.svg?style=svg&circle-token=cb6ee3f0051ca9c9d2026836be599aedd02ae894)](https://circleci.com/gh/ePages-de/jenkins-rnd-pipeline/tree/dev) | [![Circle CI](https://circleci.com/gh/ePages-de/jenkins-rnd-pipeline/tree/master.svg?style=svg&circle-token=cb6ee3f0051ca9c9d2026836be599aedd02ae894)](https://circleci.com/gh/ePages-de/jenkins-rnd-pipeline/tree/master) | [![Circle CI](https://circleci.com/gh/ePages-de/jenkins-rnd-pipeline/tree/stable.svg?style=svg&circle-token=cb6ee3f0051ca9c9d2026836be599aedd02ae894)](https://circleci.com/gh/ePages-de/jenkins-rnd-pipeline/tree/stable)|

## Short Description

1. The directory files/home/jenkins is a backup of all old XML files representing all jenkins jobs.
2. The directory jobs is for all pipeline DSL files plus reusable modules.
3. The directory public-keys contains public keys of epages members.
4. The directory scripts contains helper scripts to create the backups, and get/set the public keys.

## Testing

ToDo.