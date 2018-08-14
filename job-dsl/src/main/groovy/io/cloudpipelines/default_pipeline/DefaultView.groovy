package io.cloudpipelines.default_pipeline

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.View
import javaposse.jobdsl.dsl.views.ColumnsContext

import io.cloudpipelines.common.Coordinates
import io.cloudpipelines.common.PipelineDefaults
import io.cloudpipelines.common.PipelineDescriptor
import io.cloudpipelines.steps.Build
import io.cloudpipelines.projectcrawler.Repository

/**
 * Generates views for a project
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@CompileStatic
class DefaultView {
	private final io.cloudpipelines.common.PipelineDefaults pipelineDefaults
	private final DslFactory dsl

	DefaultView(io.cloudpipelines.common.PipelineDefaults pipelineDefaults, DslFactory dsl) {
		this.pipelineDefaults = pipelineDefaults
		this.dsl = dsl
	}

	void allViews(List<Repository> repositories) {
		dsl.listView('Seeds') {
			jobs {
				regex('.*-seed')
			}
			columns {
				defaultColumns(delegate as ColumnsContext)
			}
		}
		dsl.nestedView('Pipelines') {
			def nested = delegate
			repositories.each {
				String projectName = it.name
				nested.views {
					deliveryPipelineView(projectName) {
						allowPipelineStart()
						pipelineInstances(5)
						showAggregatedPipeline(false)
						columns(1)
						updateInterval(5)
						enableManualTriggers()
						showAvatars()
						showChangeLog()
						pipelines {
							component("Deploy ${projectName} to production", Build.stepName(DefaultPipelineDefaults.projectName(projectName)))
						}
						allowRebuild()
						showDescription()
						showPromotions()
						showTotalBuildTime()
						configureAdditionalFields(delegate as View)
					}
				}
			}
		}
	}

	@CompileDynamic
	private void configureAdditionalFields(View context) {
		context.configure {
			(it / 'showTestResults').setValue(true)
			(it / 'pagingEnabled').setValue(true)
		}
	}

	private void defaultColumns(ColumnsContext context) {
		context.with {
			status()
			name()
			lastSuccess()
			lastFailure()
			lastBuildConsole()
			buildButton()
		}
	}
}
