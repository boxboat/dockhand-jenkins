package com.boxboat.jenkins.test.pipeline

import org.junit.Before
import org.junit.Test

class DeploymentTest extends PipelineBase {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void deploymentTest() throws Exception {
        def script = loadScript("${this.scriptBase}deployment.jenkins")
        script.execute()
        printCallStack()
    }
}
