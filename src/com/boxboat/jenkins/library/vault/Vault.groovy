package com.boxboat.jenkins.library.vault

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config

class Vault extends BaseConfig implements Serializable {

    int kvVersion

    String roleIdCredential

    String secretIdCredential

    String tokenCredential

    String url

    def withCredentials(Closure closure) {
        List<Object> credentials = []
        if (roleIdCredential){
            credentials.add(Config.pipeline.string(credentialsId: roleIdCredential, variable: 'VAULT_ROLE_ID',))
        }
        if (secretIdCredential){
            credentials.add(Config.pipeline.string(credentialsId: secretIdCredential, variable: 'VAULT_SECRET_ID',))
        }
        if (tokenCredential){
            credentials.add(Config.pipeline.string(credentialsId: tokenCredential, variable: 'VAULT_TOKEN',))
        }
        Config.pipeline.withCredentials(credentials) {
            closure()
        }
    }

}
