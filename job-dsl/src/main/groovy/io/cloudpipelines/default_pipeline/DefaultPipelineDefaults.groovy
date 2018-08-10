package io.cloudpipelines.default_pipeline

import groovy.transform.CompileStatic

/**
 * Contains default values for names of jobs and views for the default
 * cloud pipelines approach (cloud pipelines deploys the apps)
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@CompileStatic
class DefaultPipelineDefaults {
	static String projectName(String gitRepoName) {
		return "${gitRepoName}-pipeline"
	}

	static String viewName(String gitRepoName) {
		return gitRepoName
	}
}
