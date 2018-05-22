multibranchPipelineJob('babl') {
	branchSources {
		branchSource {
			source {
				git {
					remote 'git://git.gnome.org/babl'
					traits {
						branchDiscoveryTrait()
						headWildcardFilter {
							includes 'master'
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
						refSpecsSCMSourceTrait {
							templates {
								refSpecTemplate {
									value '+refs/heads/*:refs/remotes/@{remote}/*'
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
