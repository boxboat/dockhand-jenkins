package com.boxboat.jenkins.library.gcloud

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.deployTarget.IDeployTarget
import com.boxboat.jenkins.library.Utils

class GCloudGKEDeployTarget extends BaseConfig<GCloudGKEDeployTarget> implements IDeployTarget, Serializable {

    String gCloudAccountKey

    String name

    String project

    String region

    String zone

    @Override
    void withCredentials(Closure closure) {
        Config.global.getGCloudAccount(gCloudAccountKey).withCredentials {
            Utils.withTmpDir("KUBEHOME") {
                def projectSwitch = project ? """
                    --project="${project}"
                """.trim() : ""
                def regionSwitch = region ? """
                    --region="${region}"
                """.trim() : ""
                def zoneSwitch = zone ? """
                    --zone="${zone}"
                """.trim() : ""
                Config.pipeline.withEnv(["KUBECONFIG=${Config.pipeline.env["KUBEHOME"]}/kube.config"]) {
                    Config.pipeline.sh """
                        gcloud container clusters get-credentials "${name}" ${projectSwitch} ${regionSwitch} ${zoneSwitch}
                    """
                    closure()
                }
            }
        }
    }

}
