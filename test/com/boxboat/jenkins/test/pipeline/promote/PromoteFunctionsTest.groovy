package com.boxboat.jenkins.test.pipeline.promote

import com.boxboat.jenkins.test.pipeline.PipelineBase
import org.junit.Before
import org.junit.Test

class PromoteFunctionsTest extends PipelineBase {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void promoteFunctionsTest() throws Exception {
        def script = loadScript("${this.scriptBase}promoteFunctions.jenkins")
        script.execute()
        printCallStack()
    }
}
