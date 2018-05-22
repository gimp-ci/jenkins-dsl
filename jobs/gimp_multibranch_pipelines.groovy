/*
 * Generate a multibranch job based on settings.
 */
def generatePipelineJob(String jobName, Map settings) {
    String friendly_name = settings['name'] ?: jobName
    String about_job = (settings['description'] ?: '').trim()
    String job_remote  = settings['remote'] ?: "git://git.gnome.org/${jobName}"
    Boolean support_filter_branches = 'branches' in settings
    Boolean support_filter_tags = 'tags' in settings
    String custom_filter = [settings['branches'], settings['tags']].collect { it }.join(' ')
    this.multibranchPipelineJob(jobName) {
        displayName friendly_name
        description about_job
        branchSources {
            branchSource {
                source {
                    git {
                        id 'git-scm'
                        remote job_remote
                        traits {
                            branchDiscoveryTrait()
                            tagDiscoveryTrait()
                            headWildcardFilter {
                                includes custom_filter
                                excludes ''
                            }
                            cloneOptionTrait {
                                extension {
                                    shallow false
                                    noTags true
                                    reference "/export/${jobName}.git"
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
        description: 'Development builds for the <a href="http://gegl.org/babl/">BABL library</a>.',
        branches: 'master'
    ],
    gegl: [
        branches: 'master gegl-0-2'
    ],
    libmypaint: [
        remote: 'https://github.com/mypaint/libmypaint',
        tags: 'v1.3.0'
    ],
    'mypaint-brushes': [
        remote: 'https://github.com/Jehan/mypaint-brushes',
        branches: 'v1.3.x'
    ],
    gimp: [
        branches: 'master gimp-2-10 gimp-2-8'
    ]
]

multibranch_jobs.each { String name, Map settings ->
    String description_addon = '  Source code for this job and build pipeline can be viewed at the <a href="https://github.com/gimp-ci/jenkins-dsl">Jenkins DSL Code</a> repository.'
    settings['description'] = (settings['description']?: '') + description_addon
    generatePipelineJob name, settings
}
