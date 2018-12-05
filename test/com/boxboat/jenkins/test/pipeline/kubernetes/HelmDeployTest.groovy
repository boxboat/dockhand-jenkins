package com.boxboat.jenkins.test.pipeline.kubernetes


import com.boxboat.jenkins.pipeline.kubernetes.HelmDeploy
import org.junit.Test

import static org.junit.Assert.assertEquals

class HelmDeployTest {

    @Test
    void testHelmDeploy() {
        def helmDeploy = new HelmDeploy(
                directory: "./charts",
                name: "test",
                chart: ".",
                options: [
                        "f": "values1.yaml"
                ]
        )
        def installScript = helmDeploy.installScript(["f": "values2.yaml"])
        assertEquals(installScript.trim(), """
            helm_current_dir=\$(pwd)
            cd "./charts"
            helm install -f "values1.yaml" -f "values2.yaml" --name "test" "."
            cd "\$helm_current_dir"
        """.trim())
        def upgradeScript = helmDeploy.upgradeScript(["f": "values3.yaml", "install": true])
        assertEquals(upgradeScript.trim(), """
            helm_current_dir=\$(pwd)
            cd "./charts"
            helm upgrade -f "values1.yaml" -f "values3.yaml" --install "test" "."
            cd "\$helm_current_dir"
        """.trim())
    }

}
