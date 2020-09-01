package com.boxboat.jenkins.test.pipeline.promote

import com.boxboat.jenkins.test.pipeline.PipelineBase
import org.junit.Before
import org.junit.Test

class PromoteCustomVersionMismatchTest extends PipelineBase {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void promoteCustomVersionMismatchTest() throws Exception {
        def script = loadScript("${this.scriptBase}promoteCustomVersionMismatch.jenkins")
        script.execute()
        printCallStack()
    }
}
