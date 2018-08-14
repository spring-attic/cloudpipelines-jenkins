package io.cloudpipelines.spinnaker

import groovy.transform.CompileStatic
import javaposse.jobdsl.dsl.DslFactory

import io.cloudpipelines.common.Coordinates
import io.cloudpipelines.common.PipelineDefaults
import io.cloudpipelines.spinnaker.SpinnakerDefaults
import io.cloudpipelines.projectcrawler.Repository

/**
 * Default view for Spinnaker
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@CompileStatic
class SpinnakerDefaultView {
	private final DslFactory dsl
	private final io.cloudpipelines.common.PipelineDefaults pipelineDefaults

	SpinnakerDefaultView(DslFactory dsl, io.cloudpipelines.common.PipelineDefaults pipelineDefaults) {
		this.dsl = dsl
		this.pipelineDefaults = pipelineDefaults
	}

	void view(List<Repository> repositories) {
		dsl.nestedView('Spinnaker') {
			repositories.each { Repository repo ->
				Coordinates coordinates = Coordinates.fromRepo(repo, pipelineDefaults)
				String viewName = SpinnakerDefaults.viewName(coordinates.gitRepoName)
				String gitRepoName = SpinnakerDefaults.projectName(coordinates.gitRepoName)
				views {
					listView(viewName) {
						jobs {
							regex("${gitRepoName}.*")
						}
						columns {
							status()
							name()
							lastSuccess()
							lastFailure()
							lastBuildConsole()
							buildButton()
						}
					}
				}
			}
		}
	}
}
