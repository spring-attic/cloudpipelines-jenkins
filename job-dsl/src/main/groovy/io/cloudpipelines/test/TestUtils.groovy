package io.cloudpipelines.test

import groovy.transform.CompileStatic

import io.cloudpipelines.projectcrawler.Repository

/**
 * Utility class helping to test the scripts
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@CompileStatic
class TestUtils {

	public static List<Repository> TEST_REPO = [new Repository("foo", "git@bar.com:baz/foo.git", "http://bar.com/baz/foo.git", "master")]
}
