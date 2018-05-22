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
                '../babl/master'
            ]
        ],
        libmypaint: [
            'v1.3.0': [
                '../babl/master',
                '../gegl/master'
            ]
        ],
        'mypaint-brushes': [
            'v0.3.x': [
                '../babl/master',
                '../gegl/master',
                '../libmypaint/v1.3.0'
            ]
        ],
        gimp: [
            'master': [
                '../babl/master',
                '../gegl/master',
                '../libmypaint/v1.3.0',
                '../mypaint-brushes/v0.3.x'
            ],
            'gimp-2-10': [
                '../babl/master',
                '../gegl/master',
                '../libmypaint/v1.3.0',
                '../mypaint-brushes/v0.3.x'
            ],
            'gimp-2-8': [
                '../babl/master',
                '../gegl/gegl-0-2',
                '../libmypaint/v1.3.0',
                '../mypaint-brushes/v0.3.x'
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
    String myEnv = "-e ${project.toUpperCase()}_BRANCH=${env.BRANCH_NAME}" + getDockerEnv(project, env.BRANCH_NAME)
    node('master') {
        stage("Environment") {
            docker.image('gimp/gimp:latest').inside("${myEnv} -v gimp-git-data:/export:ro") {
                environment_string = sh(script: 'env | LC_ALL=C sort | grep -E \'BRANCH|^BUILD_|^JOB_|PATH|^DEBIAN_FRONTEND|^PREFIX|ACLOCAL_FLAGS|^PWD\'', returnStdout: true).split('\n').join('\n    ')
                echo "DOCKER ENVIRONMENT:\n    ${environment_string}"
            }
        }
        docker.image('gimp/gimp:latest').inside("${myEnv} -v gimp-git-data:/export:ro") {
            stage('Copy Dependencies') {
                for(String dependency : projectDependencies(project, env.BRANCH_NAME)) {
                    copyArtifacts fingerprintArtifacts: true, flatten: true, projectName: dependency, selector: lastSuccessful()
                    sh 'mv *.tar.gz /data/'
                }
            }
            stage("Build ${getFriendlyName(project)}") {
                //automatically generated checkout command from pipeline syntax generator
                checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'refs/heads/master']], browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/gimp-ci/docker-jenkins-gimp'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'ChangelogToBranch', options: [compareRemote: 'origin', compareTarget: 'master']], [$class: 'RelativeTargetDirectory', relativeTargetDir: 'docker-jenkins-gimp']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/gimp-ci/docker-jenkins-gimp']]]
                //end automatically generated checkout

                sh "bash ./docker-jenkins-gimp/debian-testing/${project}.sh"
            }
            stage("Publish artifacts") {
                archiveArtifacts artifacts: "${project}/${project}-internal.tar.gz", fingerprint: true, onlyIfSuccessful: true
            }
        }
        deleteDir()
    }
}
