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
projectEnvMap = [
    gimp: [
        master: [
            BABL_BRANCH: master,
            GEGL_BRANCH: master
        ],
        'gimp-2-10': [
            BABL_BRANCH
        ]
    ]
]

/*
String getDockerEnv(String project, branch) {
}
*/

def call() {
    //e.g. project = babl
    String project = env.JOB_NAME.tokenize('/')[0]
    String myEnv = "-e ${project.toUpperCase()}_BRANCH=${env.BRANCH_NAME}"
    node('master') {
        stage("Environment") {
            docker.image('gimp/gimp:latest').inside("${myEnv} -v gimp-git-data:/export:ro") {
                environment_string = sh(script: 'env | LC_ALL=C sort | grep -E \'BRANCH|^BUILD_|^JOB_|PATH|^DEBIAN_FRONTEND|^PREFIX|ACLOCAL_FLAGS|^PWD\'', returnStdout: true).split('\n').join('\n    ')
                echo "DOCKER ENVIRONMENT:\n    ${environment_string}"
            }
        }
        stage("Build ${project}") {
        }
    }
}
