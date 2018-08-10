package io.cloudpipelines.spinnaker.pipeline.steps

import groovy.transform.CompileStatic
import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.helpers.ScmContext
import javaposse.jobdsl.dsl.helpers.publisher.PublisherContext
import javaposse.jobdsl.dsl.helpers.step.StepContext
import javaposse.jobdsl.dsl.helpers.wrapper.WrapperContext
import javaposse.jobdsl.dsl.jobs.FreeStyleJob

import io.cloudpipelines.common.BashFunctions
import io.cloudpipelines.common.Coordinates
import io.cloudpipelines.common.EnvironmentVariables
import io.cloudpipelines.common.PipelineDefaults
import io.cloudpipelines.common.PipelineDescriptor
import io.cloudpipelines.steps.CommonSteps
import io.cloudpipelines.steps.CreatedJob
import io.cloudpipelines.steps.Step

/**
 * Pushes production tag
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@CompileStatic
class ProdSetTag implements Step<FreeStyleJob> {
	private final DslFactory dsl
	private final io.cloudpipelines.common.PipelineDefaults pipelineDefaults
	private final BashFunctions bashFunctions
	private final CommonSteps commonSteps

	ProdSetTag(DslFactory dsl, io.cloudpipelines.common.PipelineDefaults pipelineDefaults) {
		this.dsl = dsl
		this.pipelineDefaults = pipelineDefaults
		this.bashFunctions = pipelineDefaults.bashFunctions()
		this.commonSteps = new CommonSteps(this.pipelineDefaults, this.bashFunctions)
	}

	@Override
	CreatedJob step(String projectName, Coordinates coordinates, PipelineDescriptor descriptor) {
		String gitRepoName = coordinates.gitRepoName
		String fullGitRepo = coordinates.fullGitRepo
		Job job = dsl.job("${projectName}-prod-tag-repo") {
			deliveryPipelineConfiguration('Prod', 'Tag the repo')
			environmentVariables(pipelineDefaults.defaultEnvVars as Map<Object, Object>)
			parameters {
				stringParam(EnvironmentVariables.PIPELINE_VERSION_ENV_VAR, "", "Version of the project to run the tests against")
			}
			wrappers {
				commonSteps.defaultWrappers(delegate as WrapperContext)
				commonSteps.deliveryPipelineVersion(delegate as WrapperContext)
			}
			scm {
				commonSteps.configureScm(delegate as ScmContext, fullGitRepo,
					"dev/${gitRepoName}/\${${EnvironmentVariables.PIPELINE_VERSION_ENV_VAR}}")
			}
			commonSteps.gitEmail(delegate as Job)
			publishers {
				git {
					forcePush(true)
					pushOnlyIfSuccess()
					tag('origin', "prod/${gitRepoName}/\${${EnvironmentVariables.PIPELINE_VERSION_ENV_VAR}}") {
						create()
						update()
					}
				}
			}
		}
		customize(job)
		return new CreatedJob(job, false)
	}

	@Override void customize(FreeStyleJob job) {
		commonSteps.customizers().each {
			it.customizeAll(job)
			it.customizeProd(job)
		}
	}
}
