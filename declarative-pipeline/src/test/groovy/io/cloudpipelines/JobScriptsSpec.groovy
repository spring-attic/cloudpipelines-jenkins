package io.cloudpipelines

import groovy.io.FileType
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.GeneratedItems
import javaposse.jobdsl.dsl.MemoryJobManagement
import javaposse.jobdsl.dsl.ScriptRequest
import spock.lang.Specification
import spock.lang.Unroll
/**
 * Tests that all dsl scripts in the jobs directory will compile.
 */
class JobScriptsSpec extends Specification {

	URL sample = JobScriptsSpec.getResource("/Jenkinsfile-sample")

	@Unroll
	def 'should compile script #file.name'() {
		given:

		MemoryJobManagement jm = new MemoryJobManagement()
		defaultStubbing(jm)
		jm.parameters << [
				SCRIPTS_DIR: 'foo',
				JENKINSFILE_DIR: 'foo',
				TEST_MODE_DESCRIPTOR: ''
		]
		DslScriptLoader loader = new DslScriptLoader(jm)

		when:
		GeneratedItems scripts = loader.runScripts([new ScriptRequest(file.text)])

		then:
		noExceptionThrown()

		where:
		file << jobFiles()
	}

	private void defaultStubbing(MemoryJobManagement jm) {
		jm.availableFiles['foo/Jenkinsfile-sample'] = sample.text
	}

	static List<File> jobFiles() {
		List<File> files = []
		new File('jobs').eachFileRecurse(FileType.FILES) {
			files << it
		}
		return files
	}

}

