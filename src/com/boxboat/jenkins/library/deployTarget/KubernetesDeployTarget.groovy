package com.boxboat.jenkins.library.deployTarget

import com.boxboat.jenkins.library.config.BaseConfig

class KubernetesDeployTarget extends BaseConfig<KubernetesDeployTarget> implements IDeployTarget {

    String caCertificate

    String contextName

    String credential

    String serverUrl

    @Override
    void withCredentials(steps, closure) {
        steps.withKubeConfig(
                credentialsId: credential,
                caCertificate: caCertificate,
                serverUrl: serverUrl,
                contextName: contextName
        ) {
            closure()
        }
    }

}
