

node('build-vm') {

    withEnv(["WORKSPACE=${pwd()}"]) {

        stage('get_cartridges') {

            checkout changelog: false,
                     poll: false,
                     scm: [
                            $class: 'GitSCM',
                            branches: [[name: '*/develop']],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'AllCartridges/Cartridges']],
                            submoduleCfg: [],
                            userRemoteConfigs: [[credentialsId: '596d6633-1bb8-4ba9-9a9d-b4c652127182', url: 'git@github.com:ePages-de/Cartridges.git']]
                          ]

            checkout changelog: false,
                     poll: false,
                     scm: [
                            $class: 'GitSCM',
                            branches: [[name: '*/master']],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'AllCartridges/1und1Basic']],
                            submoduleCfg: [],
                            userRemoteConfigs: [[credentialsId: 'c24e751f-8d24-4c11-b02e-a6428c3f88b7', url: 'git@am-git.intern.epages.de:am-provider/1und1Basic.git']]
                          ]

            checkout changelog: false,
                     poll: false,
                     scm: [
                            $class: 'GitSCM',
                            branches: [[name: '*/master']],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'AllCartridges/Strato']],
                            submoduleCfg: [],
                            userRemoteConfigs: [[credentialsId: 'c24e751f-8d24-4c11-b02e-a6428c3f88b7', url: 'git@am-git.intern.epages.de:am-provider/Strato.git']]
                          ]

            checkout changelog: false,
                     poll: false,
                     scm: [
                            $class: 'GitSCM',
                            branches: [[name: '*/master']],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'AllCartridges/ApplicationManagement']],
                            submoduleCfg: [],
                            userRemoteConfigs: [[credentialsId: 'c24e751f-8d24-4c11-b02e-a6428c3f88b7', url: 'git@am-git.intern.epages.de:am-provider/ApplicationManagement.git']]
                          ]
            }

        stage('get_esf') {

            checkout changelog: false,
                     poll: false,
                     scm: [
                            $class: 'GitSCM',
                            branches: [[name: '*/pre-release-development']],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'esf']],
                            submoduleCfg: [],
                            userRemoteConfigs: [[credentialsId: '596d6633-1bb8-4ba9-9a9d-b4c652127182', url: 'git@github.com:ePages-de/esf.git']]
                          ]
        }

        stage('build_translation_db') {

            dir('esf') {

                sh "docker run --rm -t -v ${pwd()}:/opt/build" +
                   " -v ${env.WORKSPACE}/AllCartridges:/opt/build/AllCartridges" +
                   " -v ${pwd()}/esf-epages6:/opt/esf-epages6 " +
                   "epages/oracle-jdk8-fontconfig-freetype:latest /bin/bash -c 'cd /opt/build;" +
                   " ./gradlew createTranslationDB -PmyPath=/opt/build/AllCartridges;'"
            }
        }

        stage('publish_db') {

            dir('esf') {

                withCredentials([[$class: 'UsernamePasswordMultiBinding',
                    credentialsId: 'd0ce56bf-e4a4-4a33-9a1b-7a6fd1ab2f0c',
                    passwordVariable: 'PASS',
                    usernameVariable: 'USER']]) {

                    sh "docker run --rm -t -v ${pwd()}:/opt/build -v ${pwd()}/esf-epages6:/opt/esf-epages6 " +
                       "epages/oracle-jdk8-fontconfig-freetype:latest /bin/bash -c 'cd /opt/build;" +
                       " ./gradlew -PartifactsUser=${env.USER} -PartifactsPswd=${env.PASS} -PpublishTranslation :esf-epages6:publish;'"
                }
            }
        }
    }
}
