package com.boxboat.jenkins.library.gcloud

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.deployTarget.IDeployTarget

class GCloudGKEDeployTarget extends BaseConfig<GCloudGKEDeployTarget> implements IDeployTarget, Serializable {

    String gCloudAccountKey

    String name

    String project

    String region

    String zone

    @Override
    void withCredentials(closure) {
        Config.global.getGCloudAccount(gCloudAccountKey).withCredentials {
            def tempDir = Config.pipeline.sh(returnStdout: true, script: """
                mktemp -d
            """)?.trim()
            try {
                def projectSwitch = project ? """
                    --project="${project}"
                """.trim() : ""
                def regionSwitch = region ? """
                    --region="${region}"
                """.trim() : ""
                def zoneSwitch = zone ? """
                    --zone="${zone}"
                """.trim() : ""
                Config.pipeline.withEnv(["KUBECONFIG=${tempDir}/kube.config"]) {
                    Config.pipeline.sh """
                        gcloud container clusters get-credentials "${name}" ${projectSwitch} ${regionSwitch} ${zoneSwitch}
                    """
                    closure()
                }
            } finally {
                Config.pipeline.sh """
                    rm -rf "${tempDir}"
                """
            }
        }
    }

}
