package com.boxboat.jenkins.library.deployTarget

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config

class KubernetesDeployTarget extends BaseConfig implements IDeployTarget, Serializable {

    String caCertificate

    String contextName

    String credential

    String serverUrl

    @Override
    void withCredentials(closure) {
        Config.pipeline.withKubeConfig(
                credentialsId: credential,
                caCertificate: caCertificate,
                serverUrl: serverUrl,
                contextName: contextName
        ) {
            closure()
        }
    }

}
