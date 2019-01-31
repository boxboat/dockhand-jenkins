package com.boxboat.jenkins.library.registry

import com.boxboat.jenkins.library.config.Config

class GCRRegistry extends Registry implements Serializable {

    def withCredentials(closure) {
        Config.pipeline.withCredentials([
                Config.pipeline.file(credentialsId: credential, variable: 'gCloudKeyFile')
        ]) {
            Config.pipeline.sh """
                    gcloud auth activate-service-account --key-file "${Config.pipeline.env.gCloudKeyFile}"
                    gcloud auth configure-docker --quiet
                """
            closure()
            Config.pipeline.sh """
                gcloud auth revoke
            """
        }
    }

}
