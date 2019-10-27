package com.boxboat.jenkins.library.gcloud

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config

class GCloudAccount extends BaseConfig<GCloudAccount> implements Serializable {

    String account

    String keyFileCredential

    def withCredentials(Closure closure) {
        def tempDir = Config.pipeline.sh(returnStdout: true, script: """
            mktemp -d
        """)?.trim()
        try {
            Config.pipeline.withEnv(["CLOUDSDK_CONFIG=${tempDir}"]) {
                Config.pipeline.withCredentials([Config.pipeline.file(credentialsId: keyFileCredential, variable: 'GCLOUD_KEY_FILE')]) {
                    Config.pipeline.sh """
                        gcloud auth activate-service-account "${account}" --key-file="\$GCLOUD_KEY_FILE"
                    """
                }
                closure()
            }
        } finally {
            Config.pipeline.sh """
                rm -rf "${tempDir}"
            """
        }
    }

}
