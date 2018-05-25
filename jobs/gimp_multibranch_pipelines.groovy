/*
 * This script is for Generating core jobs related to building GIMP.
 */

import jenkins.model.Jenkins

/*
   Generate a multibranch job based on settings.
 */
def generatePipelineJob(String jobName, Map settings) {
    String friendly_name = settings['name'] ?: jobName
    String about_job = (settings['description'] ?: '').trim()
    String job_remote  = settings['remote'] ?: "https://gitlab.gnome.org/GNOME/${jobName}"
    Boolean support_filter_branches = 'branches' in settings
    Boolean support_filter_tags = 'tags' in settings
    String custom_filter = [settings['branches'], settings['tags']].findAll { it }.join(' ')
    multibranchPipelineJob(jobName) {
        displayName friendly_name
        description about_job
        branchSources {
            branchSource {
                source {
                    git {
                        id 'git-scm'
                        remote job_remote
                        traits {
                            //gitBranchDiscovery and gitTagDiscovery relies on unstable DSL.  See https://github.com/gimp-ci/jenkins-dsl/issues/1
                            gitBranchDiscovery()
                            gitTagDiscovery()
                            headWildcardFilter {
                                includes custom_filter
                                excludes ''
                            }
                            cloneOptionTrait {
                                extension {
                                    shallow false
                                    noTags true
                                    reference "${Jenkins.instance.root}/export/${jobName}.git"
                                    timeout 30
                                }
                            }
                            if(support_filter_branches || support_filter_tags) {
                                refSpecsSCMSourceTrait {
                                    templates {
                                        if(support_filter_branches) {
                                            refSpecTemplate {
                                                value '+refs/heads/*:refs/remotes/@{remote}/*'
                                            }
                                        }
                                        if(support_filter_tags) {
                                            refSpecTemplate {
                                                value '+refs/tags/*:refs/tags/*'
                                            }
                                        }
                                    }
                                }
                            }
                            wipeWorkspaceTrait()
                        }
                    }
                }
            }
        }
        orphanedItemStrategy {
            discardOldItems {
                numToKeep 30
            }
        }
        factory {
            pipelineBranchDefaultsProjectFactory {
                scriptPath 'Jenkinsfile'
            }
        }
        triggers {
            periodic 30
        }
    }
}

/*
 * List of all all GIMP pipeline jobs in a basic Map format.
 */
Map multibranch_jobs = [
    babl: [
        name: 'BABL branch builds',
        description: '<p>Development builds for the <a href="http://gegl.org/babl/">BABL library</a>.  This project is a dependency of <a href="/job/gimp">GIMP</a>.</p>',
        branches: 'master'
    ],
    gegl: [
        name: 'GEGL branch builds',
        description: '<p>Development builds for the <a href="http://gegl.org/">GEGL library</a>.  This project is a dependency of <a href="/job/gimp">GIMP</a>.</p>',
        branches: 'master gegl-0-2'
    ],
    libmypaint: [
        description: '<p>Development builds for the <a href="https://github.com/mypaint/libmypaint/tree/v1.3.0">libmypaint library</a>.  Specifically libmypaint git tag v1.3.0.  This project is a dependency of <a href="/job/gimp">GIMP</a>.</p>',
        remote: 'https://github.com/mypaint/libmypaint',
        tags: 'v1.3.0'
    ],
    'mypaint-brushes': [
        description: '<p>Development builds for the <a href="https://github.com/Jehan/mypaint-brushes/tree/v1.3.x">mypaint-brushes library</a>.  Specifically mypaint-brushes git branch v1.3.x.  This project is a dependency of <a href="/job/gimp">GIMP</a>.</p>',
        remote: 'https://github.com/Jehan/mypaint-brushes',
        branches: 'v1.3.x'
    ],
    gimp: [
        name: 'GIMP branch builds',
        description: '''
<p>Development builds for <a href="http://www.gimp.org/">GIMP</a> (GNU Image Manipulation Program).</p>
<p>This project depends on the following projects:</p>
<ul>
    <li><a href="/job/babl">BABL</a></li>
    <li><a href="/job/gegl">GEGL</a></li>
    <li><a href="/job/libmypaint">libmypaint</a></li>
    <li><a href="/job/mypaint-brushes">mypaint-brushes</a></li>
</ul>
''',
        branches: 'master gimp-2-10 gimp-2-8'
    ]
]

multibranch_jobs.each { String name, Map settings ->
    String about_project = '<h1>About this project</h1>\n'
    String description_addon = '\n' + '''
<p>Source code for this job and build pipeline can be viewed at the <a href="https://github.com/gimp-ci/jenkins-dsl">Jenkins DSL Code</a> repository.</p>
<h1>Rendered pipeline</h1>
<p>To see a beautifully rendered build pipeline for this project click the "Open Blue Ocean" link in the menu to the left.  It is the next generating user interface for Jenkins.</a>
'''.trim()
    settings['description'] = about_project + (settings['description']?: '') + description_addon
    generatePipelineJob name, settings
}
