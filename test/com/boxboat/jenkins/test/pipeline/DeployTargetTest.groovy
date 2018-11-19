package com.boxboat.jenkins.test.pipeline

import org.junit.Before
import org.junit.Test

class DeployTargetTest extends PipelineBase {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void repoPipelineTest() throws Exception {
        def script = loadScript("${this.scriptBase}deployTarget.jenkins")
        script.execute()
        printCallStack()
    }
}
