package com.boxboat.jenkins.test.pipeline.deploy.kubernetes

import com.boxboat.jenkins.pipeline.deploy.kubernetes.KubePod
import org.junit.Test

import static org.junit.Assert.assertEquals

class KubePodTest {

    @Test
    void testKubeLogs() {
        def kubeLogs = KubePod.pollScript(outFile: "out.log", namespace: "test-ns", container: "nginx", labels: "a=b,c=d")
        assertEquals("""
            ${System.getProperty('java.io.tmpdir')}/sharedLibraryScripts/pod-logs.sh -o "out.log" -n "test-ns" -l "a=b,c=d" -c "nginx"
        """.trim(), kubeLogs.trim())
    }

    @Test
    void testKubeExec() {
        def kubeExec = KubePod.execScript(namespace: "test-ns", labels: "a=b,c=d", container: "nginx", command: ["cat", "test.yaml"])
        assertEquals("""
            ${System.getProperty('java.io.tmpdir')}/sharedLibraryScripts/pod-exec.sh -n "test-ns" -l "a=b,c=d" -c "nginx" "cat" "test.yaml"
        """.trim(), kubeExec.trim())
    }

}
