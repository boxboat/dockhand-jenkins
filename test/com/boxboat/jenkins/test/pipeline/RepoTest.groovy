package com.boxboat.jenkins.test.pipeline

import org.junit.Before
import org.junit.Test

class RepoTest extends PipelineBase {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void repoPipelineTest() throws Exception {
        def script = loadScript("${this.scriptBase}repo.jenkins")
        script.execute()
        printCallStack()
    }
}
