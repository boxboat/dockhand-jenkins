package com.boxboat.jenkins.test.pipeline.promote

import com.boxboat.jenkins.test.pipeline.PipelineBase
import org.junit.Before
import org.junit.Test

class PromoteCustomHigherVersionTest extends PipelineBase {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void promoteCustomHigherVersionTest() throws Exception {
        def script = loadScript("${this.scriptBase}promoteCustomHigherVersion.jenkins")
        script.execute()
        printCallStack()
    }
}
