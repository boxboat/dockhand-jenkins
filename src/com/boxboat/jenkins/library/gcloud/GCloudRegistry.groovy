package com.boxboat.jenkins.library.gcloud

import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.docker.Registry

class GCloudRegistry extends Registry implements Serializable {

    String gCloudAccountKey

    @Override
    def withCredentials(closure) {
        Config.global.getGCloudAccount(gCloudAccountKey).withCredentials {
            def tempDir = Config.pipeline.sh(returnStdout: true, script: """
                mktemp -d
            """)?.trim()
            try {
                Config.pipeline.withEnv(["DOCKER_CONFIG=${tempDir}"]) {
                    Config.pipeline.sh """
                        gcloud auth configure-docker --quiet
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
