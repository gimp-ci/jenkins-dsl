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
Map projectEnvMap = [
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

String getDockerEnv(String project, branch) {
    projectEnvMap[project]?.get(branch)?.collect { k, v ->
        "-e ${k}=${v}"
    }.join(' ') ?: ''
}

def call() {
    //e.g. project = gimp
    String project = env.JOB_NAME.tokenize('/')[0]
    //e.g. -e GIMP_BRANCH=master
    String myEnv = "-e ${project.toUpperCase()}_BRANCH=${env.BRANCH_NAME}" + getDockerEnv(project, env.BRANCH_NAME)
    node('master') {
        stage("Environment") {
            docker.image('gimp/gimp:latest').inside("${myEnv} -v gimp-git-data:/export:ro") {
                environment_string = sh(script: 'env | LC_ALL=C sort | grep -E \'BRANCH|^BUILD_|^JOB_|PATH|^DEBIAN_FRONTEND|^PREFIX|ACLOCAL_FLAGS|^PWD\'', returnStdout: true).split('\n').join('\n    ')
                echo "DOCKER ENVIRONMENT:\n    ${environment_string}"
            }
        }
        stage("Build ${project}") {
            docker.image('gimp/gimp:latest').inside("${myEnv} -v gimp-git-data:/export:ro") {
                //automatically generated checkout command from pipeline syntax generator
                checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'refs/heads/master']], browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/gimp-ci/docker-jenkins-gimp'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'ChangelogToBranch', options: [compareRemote: 'origin', compareTarget: 'master']], [$class: 'RelativeTargetDirectory', relativeTargetDir: 'docker-jenkins-gimp']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/gimp-ci/docker-jenkins-gimp']]]
                //end automatically generated checkout

                sh "bash ./docker-jenkins-gimp/debian-testing/${project}.sh"
                archiveArtifacts artifacts: "${project}/${project}-internal.tar.gz", fingerprint: true, onlyIfSuccessful: true
            }
        }
    }
}
