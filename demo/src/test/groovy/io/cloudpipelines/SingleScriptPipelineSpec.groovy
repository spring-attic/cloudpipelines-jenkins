package io.cloudpipelines

import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.GeneratedItems
import javaposse.jobdsl.dsl.MemoryJobManagement
import javaposse.jobdsl.dsl.ScriptRequest
import spock.lang.Specification

/**
 * Tests that all dsl scripts in the jobs directory will compile.
 */
class SingleScriptPipelineSpec extends Specification {

	//remove::start[CF]
	def 'should create seed job for CF'() {
		given:
		MemoryJobManagement jm = new MemoryJobManagement()
		DslScriptLoader loader = new DslScriptLoader(jm)

		when:
		GeneratedItems scripts = loader.runScripts([new ScriptRequest(
				new File("seed/jenkins_pipeline.groovy").text)])

		then:
		noExceptionThrown()

		and:
		scripts.jobs.collect { it.jobName }.contains("jenkins-pipeline-cf-seed")
	}
	//remove::end[CF]

	//remove::start[K8S]
	def 'should create seed job for K8s'() {
		given:
		MemoryJobManagement jm = new MemoryJobManagement()
		DslScriptLoader loader = new DslScriptLoader(jm)

		when:
		GeneratedItems scripts = loader.runScripts([new ScriptRequest(
			new File("seed/jenkins_pipeline.groovy").text)])

		then:
		noExceptionThrown()

		and:
		scripts.jobs.collect { it.jobName }.contains("jenkins-pipeline-k8s-seed")
	}
	//remove::end[K8S]

}

