package com.boxboat.jenkins.library.gcloud

import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.docker.Registry
import com.boxboat.jenkins.library.Utils

class GCloudRegistry extends Registry implements Serializable {

    String gCloudAccountKey

    @Override
    def withCredentials(Closure closure) {
        Config.global.getGCloudAccount(gCloudAccountKey).withCredentials {
            Utils.withTmpDir("DOCKER_CONFIG") {
                Config.pipeline.sh """
                    gcloud auth configure-docker --quiet
                """
                closure()
            }
        }
    }

}
