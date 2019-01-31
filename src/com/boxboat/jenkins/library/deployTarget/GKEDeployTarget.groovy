package com.boxboat.jenkins.library.deployTarget

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config

class GKEDeployTarget extends BaseConfig implements IDeployTarget, Serializable {

    String credential

    String clusterName

    String region

    String zone

    @Override
    void withCredentials(closure) {
        Config.pipeline.withCredentials(Config.pipeline.file(credentialsId: credential, variable: 'gCloudKeyFile')) {
            def clusterArgs = ""
            if (region) {
                clusterArgs = "--region \"${region}\""
            }
            if (zone) {
                clusterArgs = "--zone \"${zone}\""
            }
            Config.pipeline.sh """
                gcloud auth activate-service-account --key-file "${Config.pipeline.env.gCloudKeyFile}"
                gcloud container clusters get-credentials "${clusterName}" ${clusterArgs}
            """
            closure()
            Config.pipeline.sh """
                gcloud auth revoke
            """
        }
    }

}
