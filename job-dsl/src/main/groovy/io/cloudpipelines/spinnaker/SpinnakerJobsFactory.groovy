package io.cloudpipelines.spinnaker

import groovy.transform.CompileStatic
import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.jobs.FreeStyleJob

import io.cloudpipelines.common.BashFunctions
import io.cloudpipelines.common.Coordinates
import io.cloudpipelines.common.EnvironmentVariables
import io.cloudpipelines.common.PipelineDefaults
import io.cloudpipelines.common.PipelineDescriptor
import io.cloudpipelines.common.PipelineJobsFactory
import io.cloudpipelines.spinnaker.pipeline.SpinnakerPipelineBuilder
import io.cloudpipelines.spinnaker.pipeline.steps.ProdRemoveTag
import io.cloudpipelines.spinnaker.pipeline.steps.ProdSetTag
import io.cloudpipelines.spinnaker.pipeline.steps.StagePrepare
import io.cloudpipelines.spinnaker.pipeline.steps.TestPrepare
import io.cloudpipelines.steps.Build
import io.cloudpipelines.steps.CommonSteps
import io.cloudpipelines.steps.TestRollbackTest
import io.cloudpipelines.steps.StageTest
import io.cloudpipelines.steps.TestTest
import io.cloudpipelines.projectcrawler.Repository

/**
 * Factory for Spinnaker Jenkins jobs
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@CompileStatic
class SpinnakerJobsFactory implements PipelineJobsFactory {
	private final io.cloudpipelines.common.PipelineDefaults pipelineDefaults
	private final DslFactory dsl
	private final PipelineDescriptor descriptor
	private final Repository repository
	private final CommonSteps commonSteps

	SpinnakerJobsFactory(io.cloudpipelines.common.PipelineDefaults pipelineDefaults, PipelineDescriptor descriptor,
						 DslFactory dsl, Repository repository) {
		this.pipelineDefaults = pipelineDefaults
		this.dsl = dsl
		this.descriptor = descriptor
		this.repository = repository
		this.commonSteps = new CommonSteps(pipelineDefaults, new BashFunctions(pipelineDefaults))
	}

	@Override
	void allJobs(Coordinates coordinates, String pipelineVersion, Map<String, String> additionalFiles) {
		String gitRepoName = coordinates.gitRepoName
		String projectName = SpinnakerDefaults.projectName(gitRepoName)
		pipelineDefaults.addEnvVar("PROJECT_NAME", gitRepoName)
		println "Creating jobs and views for [${projectName}]"
		String script = commonSteps.readScript("download_latest_prod_binary.sh")
		new Build(dsl, pipelineDefaults, pipelineVersion) {
			@Override
			void customize(FreeStyleJob job) {
				job.steps {
					shell(script)
				}
				super.customize(job)
			}
		}.step(projectName, coordinates, descriptor)
		new TestPrepare(dsl, pipelineDefaults).step(projectName, coordinates, descriptor)
		new TestTest(dsl, pipelineDefaults) {
			@Override
			void customize(FreeStyleJob job) {
				setTestEnvVars(job, gitRepoName)
				super.customize(job)
			}
		}.step(projectName, coordinates, descriptor)
		new TestRollbackTest(dsl, pipelineDefaults) {
			@Override
			void customize(FreeStyleJob job) {
				setTestRollbackEnvVars(job, gitRepoName)
				super.customize(job)
			}
		}.step(projectName, coordinates, descriptor)
		new StagePrepare(dsl, pipelineDefaults).step(projectName, coordinates, descriptor)
		new StageTest(dsl, pipelineDefaults) {
			@Override
			void customize(FreeStyleJob job) {
				setStageEnvVars(job, gitRepoName)
				super.customize(job)
			}
		}.step(projectName, coordinates, descriptor)
		new ProdRemoveTag(dsl, pipelineDefaults).step(projectName, coordinates, descriptor)
		new ProdSetTag(dsl, pipelineDefaults).step(projectName, coordinates, descriptor)
		println "Dumping the json with pipeline"
		dumpJsonToFile(descriptor, repository, additionalFiles)
	}

	protected void setTestEnvVars(FreeStyleJob job, String projectName) {
		job.wrappers {
			environmentVariables {
				env(EnvironmentVariables.APPLICATION_URL_ENV_VAR, "${pipelineDefaults.cfTestSpacePrefix()}-${projectName}.${pipelineDefaults.spinnakerTestHostname()}")
				env(EnvironmentVariables.STUBRUNNER_URL_ENV_VAR, "stubrunner-test-${projectName}.${pipelineDefaults.spinnakerTestHostname()}")
				env(EnvironmentVariables.CF_SKIP_PREPARE_FOR_TESTS_ENV_VAR, "true")
			}
		}
		job.parameters {
			stringParam(EnvironmentVariables.PIPELINE_VERSION_ENV_VAR, "", "Version of the project to run the tests against")
		}
	}

	protected void setTestRollbackEnvVars(FreeStyleJob job, String projectName) {
		setTestEnvVars(job, projectName)
		job.parameters {
			stringParam(EnvironmentVariables.LATEST_PROD_VERSION_ENV_VAR, "", "Version of the project to run the tests against")
		}
	}

	protected void setStageEnvVars(FreeStyleJob job, String projectName) {
		job.wrappers {
			environmentVariables {
				env(EnvironmentVariables.APPLICATION_URL_ENV_VAR, "${projectName}-${pipelineDefaults.cfStageSpace()}.${pipelineDefaults.spinnakerStageHostname()}")
				env(EnvironmentVariables.CF_SKIP_PREPARE_FOR_TESTS_ENV_VAR, "true")
			}
		}
		job.parameters {
			stringParam(EnvironmentVariables.PIPELINE_VERSION_ENV_VAR, "", "Version of the project to run the tests against")
		}
	}

	void dumpJsonToFile(PipelineDescriptor pipeline, Repository repo, Map<String, String> additionalFiles) {
		String json = new SpinnakerPipelineBuilder(pipeline, repo, pipelineDefaults, additionalFiles)
						.spinnakerPipeline()
		File pipelineJson = new File("${pipelineDefaults.workspace()}/build", repo.name + "_pipeline.json")
		pipelineJson.createNewFile()
		pipelineJson.text = json
	}
}
