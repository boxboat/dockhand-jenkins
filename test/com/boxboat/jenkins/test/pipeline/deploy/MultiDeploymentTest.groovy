package com.boxboat.jenkins.test.pipeline.deploy

import com.boxboat.jenkins.test.pipeline.PipelineBase
import org.junit.Before
import org.junit.Test

class MultiDeploymentTest extends PipelineBase {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void deploymentTest() throws Exception {
        def script = loadScript("${this.scriptBase}multiDeployment.jenkins")
        script.execute()
        printCallStack()
    }
}
