package com.boxboat.jenkins.test.pipeline.deploy.kubernetes


import com.boxboat.jenkins.pipeline.deploy.kubernetes.KubeLogs
import org.junit.Test

import static org.junit.Assert.assertEquals

class KubeLogsTest {

    @Test
    void testKubeLogs() {
        def kubeLogs = KubeLogs.pollScript(outFile: "out.log", namespace: "test-ns", container: "nginx", labels: "a=b,c=d")
        assertEquals(kubeLogs.trim(), """
            ./sharedLibraryScripts/pod-logs.sh -o "out.log" -n "test-ns" -l "a=b,c=d" -c "nginx"
        """.trim())
    }

}
