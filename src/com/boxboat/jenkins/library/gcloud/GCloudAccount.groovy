package com.boxboat.jenkins.library.gcloud

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.Utils

class GCloudAccount extends BaseConfig<GCloudAccount> implements Serializable {

    String account

    String keyFileCredential

    def withCredentials(Closure closure) {
        Utils.withTmpDir("CLOUDSDK_CONFIG") {
            Config.pipeline.withCredentials([Config.pipeline.file(credentialsId: keyFileCredential, variable: 'GCLOUD_KEY_FILE')]) {
                Config.pipeline.sh """
                    gcloud auth activate-service-account "${account}" --key-file="\$GCLOUD_KEY_FILE"
                """
            }
            closure()
        }
    }

}
