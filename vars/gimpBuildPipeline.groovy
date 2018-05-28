/*
 * This build pipeline is designed to automatically run builds for the following projects:
 *
 *   - BABL
 *   - GEGL
 *   - libmypaint
 *   - mypaint-brushes
 *   - GIMP
 *
 * The pipeline will select appropriate build scripts depending on the project calling it.
 */

import jenkins.model.Jenkins

/**
  Does nothing except return a static Map.
 */
@NonCPS
Map projectEnvMap() {
    [
        gimp: [
            master: [
                GEGL_BRANCH: 'master'
            ],
            'gimp-2-10': [
                GEGL_BRANCH: 'master'
            ],
            'gimp-2-8': [
                GEGL_BRANCH: 'gegl-0-2'
            ]
        ]
    ]
}

/**
  List of projects and branches in which to copy dependencies from.
 */
@NonCPS
List<String> projectDependencies(String project, String branch) {
    [
        gegl: [
            master: [
                'babl/master'
            ],
            'gegl-0-2': [
                'babl/master'
            ]
        ],
        libmypaint: [
            'v1.3.0': [
                'babl/master',
                'gegl/master'
            ]
        ],
        'mypaint-brushes': [
            'v1.3.x': [
                'babl/master',
                'gegl/master',
                'libmypaint/v1.3.0'
            ]
        ],
        gimp: [
            'master': [
                'babl/master',
                'gegl/master',
                'libmypaint/v1.3.0',
                'mypaint-brushes/v1.3.x'
            ],
            'gimp-2-10': [
                'babl/master',
                'gegl/master',
                'libmypaint/v1.3.0',
                'mypaint-brushes/v1.3.x'
            ],
            'gimp-2-8': [
                'babl/master',
                'gegl/gegl-0-2',
                'libmypaint/v1.3.0',
                'mypaint-brushes/v1.3.x'
            ]
        ]
    ].get(project)?.get(branch) ?: []
}

@NonCPS
String getFriendlyName(String name) {
    [
        'babl': 'BABL',
        'gegl': 'GEGL',
        'gimp': 'GIMP'
    ].get(name) ?: name
}

/**
  References the project environment map and converts variables into Docker
  arguments for environment variables.
 */
@NonCPS
String getDockerEnv(String project, branch) {
    projectEnvMap().get(project)?.get(branch)?.collect { k, v ->
        "-e ${k}=${v}"
    }?.join(' ') ?: ''
}

/**
  This is the main execution.
 */
def call() {
    //e.g. project = gimp
    String project = env.JOB_NAME.tokenize('/')[0]
    //e.g. -e GIMP_BRANCH=master -e GEGL_BRANCH=master
    String myEnv = "-e ${project.toUpperCase()}_BRANCH=${env.BRANCH_NAME} " + getDockerEnv(project, env.BRANCH_NAME)
    String reference_repo = "${Jenkins.instance.root}/export/${project}.git"
    node('master') {
        stage("Environment") {
            docker.image('gimp/gimp:latest').inside("${myEnv}") {
                sh '''
                    |head -n1 /etc/issue
                    |id
                    |uname -rms
                    |bash --version | head -n1
                   '''.stripMargin().trim()
                //environment_string = sh(script: 'env | LC_ALL=C sort', returnStdout: true).split('\n').join('\n    ')
                grep_expr = 'BRANCH|^BUILD_|^JOB_|PATH|^DEBIAN_FRONTEND|^PREFIX|ACLOCAL_FLAGS|^PWD'
                environment_string = sh(script: "env | LC_ALL=C sort | grep -E \'${grep_expr}\'", returnStdout: true).split('\n').join('\n    ')
                echo "DOCKER ENVIRONMENT:\n    ${environment_string}"
            }
        }
        stage("Update git cache") {
            lock('update-git-cache') {
                sh """
                    |#!/bin/bash
                    |echo 'Updating local git cache for faster checkouts'
                    |function update_cached_scm {
                    |    name="\$1"
                    |    repoUrl="\$2"
                    |    localRepo="${Jenkins.instance.root}/export/\${name}.git"
                    |    if [ ! -d "\${localRepo}" ]; then
                    |        git clone --mirror "\${repoUrl}" "\${localRepo}"
                    |    fi
                    |    cd "\${localRepo}"
                    |    git remote update --prune
                    |}
                    |set -exo pipefail
                    |update_cached_scm "docker-jenkins-gimp" "https://github.com/gimp-ci/docker-jenkins-gimp"
                    |update_cached_scm "${project}" "${scm.userRemoteConfigs[0].url}"
                   """.stripMargin().trim()
            }
            //check out project to subdirectory for build
            dir(project) {
                checkout scm
            }
        }
        docker.image('gimp/gimp:latest').inside("${myEnv} -v ${reference_repo}:${reference_repo}:ro") {
            if(projectDependencies(project, env.BRANCH_NAME)) {
                stage('Copy Dependencies') {
                    for(String dependency : projectDependencies(project, env.BRANCH_NAME)) {
                        copyArtifacts fingerprintArtifacts: true, flatten: true, projectName: dependency, selector: lastSuccessful()
                        sh 'mv *.tar.gz /data/'
                    }
                }
            }
            catchError {
                stage("Build ${getFriendlyName(project)}") {
                    //automatically generated checkout command from pipeline syntax generator
                    checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'refs/heads/master']], browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/gimp-ci/docker-jenkins-gimp'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'ChangelogToBranch', options: [compareRemote: 'origin', compareTarget: 'master']], [$class: 'RelativeTargetDirectory', relativeTargetDir: 'docker-jenkins-gimp'], [$class: 'CloneOption', depth: 0, noTags: true, reference: "${Jenkins.instance.root}/export/docker-jenkins-gimp.git", shallow: false]], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/gimp-ci/docker-jenkins-gimp']]]
                    //end automatically generated checkout
                    sh "bash ./docker-jenkins-gimp/debian-testing/${project}.sh"
                }
            }
            stage("Publish artifacts") {
                //archive artifacts relative to project directory
                dir(project) {
                    archiveArtifacts artifacts: "${project}-internal.tar.gz,**/test-suite.log", fingerprint: true, onlyIfSuccessful: false
                }
            }
        }
        deleteDir()
    }
}
