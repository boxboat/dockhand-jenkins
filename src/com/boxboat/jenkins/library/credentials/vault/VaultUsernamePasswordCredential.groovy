package com.boxboat.jenkins.library.credentials.vault

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config

public class VaultUsernamePasswordCredential extends VaultBaseCredential {

    public String usernameKey

    public String passwordKey

    static class Params extends BaseConfig<Params> implements Serializable {
        String usernameVariable
        String passwordVariable
    }

    def withCredentials(Params params, closure) {
        params.usernameVariable = params.usernameVariable ?: "USERNAME"
        params.passwordVariable = params.passwordVariable ?: "PASSWORD"

        def username = getSecret(usernameKey)
        def password = getSecret(passwordKey)

        def unmaskedEnvList = [
                "${params.usernameVariable}=${username}"
        ]
        def maskedPairs = [
                [
                        var     : params.passwordVariable,
                        password: password,
                ],
        ]
        def maskedEnvList = maskedPairs.collect { "${it.var}=${it.password}" }
        Config.pipeline.wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: maskedPairs]) {
            Config.pipeline.withEnv(unmaskedEnvList + maskedEnvList) {
                closure()
            }
        }
    }

    def withCredentials(Map paramsMap, closure) {
        withCredentials(new Params().newFromObject(paramsMap), closure)
    }

    @Override
    def withCredentials(Closure closure) {
        withCredentials(new Params(), closure)
    }
}
