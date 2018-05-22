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
            docker.image('gimp/gimp:latest').withRun("-e HOME=/home/jenkins -w /home/jenkins -v gimp-git-data:/export:ro") { c ->
                //environment_string = sh(script: 'env | LC_ALL=C sort | grep -E \'BRANCH|^BUILD_|^JOB_\'', returnStdout: true).split('\n').join('\n    ')
                environment_string = sh(script: 'whoami;pwd;env | LC_ALL=C sort', returnStdout: true).split('\n').join('\n    ')
                echo "ENVIRONMENT:\n    ${environment_string}"
            }
        }
    }
}
