package io.cloudpipelines.spinnaker.pipeline.model

import groovy.transform.CompileStatic

@CompileStatic
class Artifact {
	String account
	String reference
	String pattern
	String type
}
