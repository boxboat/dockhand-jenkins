package com.boxboat.jenkins.test.pipeline.deploy.kubernetes


import com.boxboat.jenkins.pipeline.deploy.kubernetes.KubeExec
import org.junit.Test

import static org.junit.Assert.assertEquals

class KubeExecTest {

    @Test
    void testKubeExec() {
        def kubeExec = KubeExec.execScript(namespace: "test-ns", labels: "a=b,c=d", command: ["cat", "test.yaml"])
        assertEquals(kubeExec.trim(), """
            ./sharedLibraryScripts/pod-exec.sh -n "test-ns" -l "a=b,c=d" "cat" "test.yaml"
        """.trim())
    }

}
