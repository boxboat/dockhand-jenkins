package com.boxboat.jenkins.test.pipeline.common.clean

import com.boxboat.jenkins.test.pipeline.PipelineBase
import org.junit.Before
import org.junit.Test

class DTRCleanTest extends PipelineBase {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void dtrCleanTest() throws Exception {
        def script = loadScript("${this.scriptBase}cleanDTR.jenkins")
        script.execute()
        printCallStack()
    }
}
