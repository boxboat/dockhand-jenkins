package com.boxboat.jenkins.test.pipeline.promote

import com.boxboat.jenkins.test.pipeline.PipelineBase
import org.junit.Before
import org.junit.Test

class PromoteTest extends PipelineBase {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void promoteTest() throws Exception {
        def script = loadScript("${this.scriptBase}promote.jenkins")
        script.execute()
        printCallStack()
    }

    @Test
    void promoteRegctlTest() throws Exception {
        def script = loadScript("${this.scriptBase}promoteRegctl.jenkins")
        script.execute()
        printCallStack()
    }
}
