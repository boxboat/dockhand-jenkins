package com.boxboat.jenkins.test.pipeline.promote

import com.boxboat.jenkins.test.pipeline.PipelineBase
import org.junit.Before
import org.junit.Test

class PromoteCustomTest extends PipelineBase {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void promoteCustomTest() throws Exception {
        def script = loadScript("${this.scriptBase}promoteCustom.jenkins")
        script.execute()
        printCallStack()
    }
}
