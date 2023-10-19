package com.boxboat.jenkins.library.azure

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config

class AzureProfile  extends BaseConfig<AzureProfile> implements Serializable{

    String keyVaultName

    String tenantIdCredential

    String clientIdCredential

    String clientSecretKeyCredential

    def withCredentials(Closure closure) {
        List<Object> credentials = []
        if (tenantIdCredential) {
            credentials.add(Config.pipeline.string(credentialsId: tenantIdCredential, variable: 'AZURE_TENANT_ID',))
        }
        if (clientIdCredential) {
            credentials.add(Config.pipeline.string(credentialsId: clientIdCredential, variable: 'AZURE_CLIENT_ID',))
        }
        if (clientSecretKeyCredential) {
            credentials.add(Config.pipeline.string(credentialsId: clientSecretKeyCredential, variable: 'AZURE_CLIENT_SECRET',))
        }
        Config.pipeline.withCredentials(credentials) {
            closure()
        }
    }
}
