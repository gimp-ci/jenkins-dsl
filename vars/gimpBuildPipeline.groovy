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

def call() {
    node('master') {
        stage("Environment") {
            docker.image('gimp/gimp:latest').inside("-v gimp-git-data:/export:ro") {
                environment_string = sh(script: 'env | LC_ALL=C sort | grep -E \'BRANCH|^BUILD_|^JOB_|PATH|^DEBIAN_FRONTEND|^PREFIX|ACLOCAL_FLAGS|^PWD\'', returnStdout: true).split('\n').join('\n    ')
                echo "DOCKER ENVIRONMENT:\n    ${environment_string}"
            }
        }
    }
}
