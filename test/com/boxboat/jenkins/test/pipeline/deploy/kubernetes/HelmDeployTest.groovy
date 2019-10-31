package com.boxboat.jenkins.test.pipeline.deploy.kubernetes


import com.boxboat.jenkins.pipeline.deploy.kubernetes.HelmDeploy
import org.junit.Test

import static org.junit.Assert.assertEquals

class HelmDeployTest {

    @Test
    void testHelmDeploy() {
        def helmDeploy = new HelmDeploy(
                directory: "./charts",
                name: "test",
                namespace: "ns1",
                chart: ".",
                options: [
                        "f": "values1.yaml"
                ]
        )
        helmDeploy.testMajorVersion(2)
        def installScript = helmDeploy.installScript(["f": "values2.yaml"])
        assertEquals("""
            helm_current_dir=\$(pwd)
            cd "./charts"
            helm install -f "values1.yaml" -f "values2.yaml" --namespace "ns1" --name "test"  "."
            cd "\$helm_current_dir"
        """.trim(), installScript.trim())
        def upgradeScript = helmDeploy.upgradeScript(["f": "values3.yaml", "install": true])
        assertEquals("""
            helm_current_dir=\$(pwd)
            cd "./charts"
            helm upgrade -f "values1.yaml" -f "values3.yaml" --install --namespace "ns1" "test" "."
            cd "\$helm_current_dir"
        """.trim(), upgradeScript.trim())
        def deleteScript = helmDeploy.deleteScript()
        assertEquals("""
            helm delete --purge "test"
        """.trim(), deleteScript.trim())

        helmDeploy.testMajorVersion(3)
        installScript = helmDeploy.installScript(["f": "values2.yaml"])
        assertEquals("""
            helm_current_dir=\$(pwd)
            cd "./charts"
            helm install -f "values1.yaml" -f "values2.yaml" --namespace "ns1" "test" "."
            cd "\$helm_current_dir"
        """.trim(), installScript.trim())
        upgradeScript = helmDeploy.upgradeScript(["f": "values3.yaml", "install": true])
        assertEquals("""
            helm_current_dir=\$(pwd)
            cd "./charts"
            helm upgrade -f "values1.yaml" -f "values3.yaml" --install --namespace "ns1" "test" "."
            cd "\$helm_current_dir"
        """.trim(), upgradeScript.trim())
        deleteScript = helmDeploy.uninstallScript()
        assertEquals("""
            helm uninstall --namespace "ns1" "test"
        """.trim(), deleteScript.trim())
    }

}
