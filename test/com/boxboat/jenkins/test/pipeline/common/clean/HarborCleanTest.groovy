package com.boxboat.jenkins.test.pipeline.common.clean

import com.boxboat.jenkins.test.pipeline.PipelineBase
import org.junit.Before
import org.junit.Test

class HarborCleanTest extends PipelineBase {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void harborCleanTest() throws Exception {
        def script = loadScript("${this.scriptBase}cleanHarbor.jenkins")
        script.execute()
        printCallStack()
    }
}
