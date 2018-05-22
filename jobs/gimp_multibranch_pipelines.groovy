/*
 * Generate a multibranch job based on settings.
 */
def generatePipelineJob(String jobName, Map settings) {
    String friendly_name = settings['name'] ?: ''
    String about_job = (settings['description'] ?: '').trim()
    String job_remote  = settings['remote'] ?: "git://git.gnome.org/${jobName}"
    Boolean support_filter_branches = 'branches' in settings
    Boolean support_filter_tags = 'tags' in settings
    String custom_filter = [settings['branches'], settings['tags']].collect { it }.join(' ')
    multibranchPipelineJob('babl') {
        displayName friendly_name
        description about_job
        branchSources {
            branchSource {
                source {
                    git {
                        remote job_remote
                        traits {
                            if(support_filter_branches) {
                                branchDiscoveryTrait()
                            }
                            if(support_filter_tags) {
                                tagDiscoveryTrait()
                            }
                            headWildcardFilter {
                                includes custom_filter
                                excludes ''
                            }
                            cloneOptionTrait {
                                extension {
                                    shallow false
                                    noTags true
                                    reference '/export/babl.git'
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
                        }
                    }
                }
            }
        }
        orphanedItemStrategy {
            defaultOrphanedItemStrategy {
                pruneDeadBranches false
                daysToKeepStr '-1'
                numToKeepStr '30'
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
        configure {
            def folderConfig = it / 'properties' / 'org.jenkinsci.plugins.pipeline.modeldefinition.config.FolderConfig'
            folderConfig << dockerLabel('docker-host')
            folderConfig << registry()
        }
    }
}

/*
 * List of all all GIMP pipeline jobs in a basic Map format.
 */
Map multibranch_jobs = [
    babl: [
        name: 'BABL branch builds',
        description: 'Development builds for the <a href="">BABL library</a>.
        branches: 'master'
    ]
]

multibranch_jobs.each { String name, Map settings ->
    generatePipelineJob name, settings
}
