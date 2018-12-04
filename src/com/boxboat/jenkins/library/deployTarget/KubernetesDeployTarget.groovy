package com.boxboat.jenkins.library.deployTarget

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.GlobalConfig

class KubernetesDeployTarget extends BaseConfig<KubernetesDeployTarget> implements IDeployTarget {

    String caCertificate

    String contextName

    String credential

    String serverUrl

    @Override
    void withCredentials(closure) {
        GlobalConfig.pipeline.withKubeConfig(
                credentialsId: credential,
                caCertificate: caCertificate,
                serverUrl: serverUrl,
                contextName: contextName
        ) {
            closure()
        }
    }

}
