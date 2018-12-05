package com.boxboat.jenkins.test.pipeline.build

import com.boxboat.jenkins.test.pipeline.PipelineBase
import org.junit.Before
import org.junit.Test

class BuildTest extends PipelineBase {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void repoPipelineTest() throws Exception {
        def script = loadScript("${this.scriptBase}build.jenkins")
        script.execute()
        printCallStack()
    }
}
