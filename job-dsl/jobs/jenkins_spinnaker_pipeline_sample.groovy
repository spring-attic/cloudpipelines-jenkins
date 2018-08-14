import javaposse.jobdsl.dsl.DslFactory

import io.cloudpipelines.common.GeneratedJobs
import io.cloudpipelines.common.PipelineDefaults
import io.cloudpipelines.common.PipelineDescriptor
import io.cloudpipelines.common.PipelineFactory
import io.cloudpipelines.common.PipelineJobsFactory
import io.cloudpipelines.common.PipelineJobsFactoryProvider
import io.cloudpipelines.spinnaker.SpinnakerDefaultView
import io.cloudpipelines.spinnaker.SpinnakerJobsFactory
import io.cloudpipelines.test.TestUtils
import io.cloudpipelines.projectcrawler.OptionsBuilder
import io.cloudpipelines.projectcrawler.Repository
import io.cloudpipelines.projectcrawler.ProjectCrawler

/**
 *  This script contains logic that
 *
 *  - uses the Project Crawler to crawl repos from an organization
 *  - for each repo picks the cloud-pipelines.yml descriptor
 *  - basing on the contents of that repo generates proper Spinnaker deployment pipeline JSON
 *  - generates Jenkins jobs required by Spinnaker
 *  - generates views for all projects
 */


DslFactory dsl = this

// These will be taken either from seed or global variables
PipelineDefaults defaults = new PipelineDefaults(binding.variables)
String pipelineVersion = defaults.pipelineVersion()
String org = defaults.repoOrganization() ?: "sc-pipelines"
String repoType = defaults.repoManagement() ?: "GITHUB"
String urlRoot = defaults.repoUrlRoot() ?: "https://github.com"

// crawl the org
ProjectCrawler crawler = new ProjectCrawler(OptionsBuilder
	.builder().rootUrl(urlRoot)
	.username(defaults.gitUsername())
	.password(defaults.gitPassword())
	.token(defaults.gitToken())
	.exclude(defaults.repoProjectsExcludePattern())
	.repository(repoType).build())

// get the repos from the org
List<Repository> repositories = defaults.testModeDescriptor() != null ?
	TestUtils.TEST_REPO : crawler.repositories(org)

// generate jobs and store errors
GeneratedJobs generatedJobs = new PipelineFactory(
	new PipelineJobsFactoryProvider() {
		@Override
		PipelineJobsFactory get(PipelineDefaults pipelineDefaults, DslFactory dslFactory, PipelineDescriptor descriptor, Repository repository) {
			return new SpinnakerJobsFactory(pipelineDefaults, descriptor, dslFactory, repository)
		}
		@Override
		List<String> additionalFiles() {
			return ["manifest.yml"]
		}
	}, defaults, crawler, dsl).generate(repositories, org, pipelineVersion)

if (generatedJobs.hasErrors()) {
	println "\n\n\nWARNING, THERE WERE ERRORS WHILE TRYING TO BUILD PROJECTS\n\n\n"
	generatedJobs.errors.each { String key, Exception error ->
		println "Exception for project [${key}], [${error}]"
		println "Stacktrace:"
		error.printStackTrace()
	}
}

// build the views
new SpinnakerDefaultView(dsl, defaults).view(generatedJobs.repositoriesForViews)
