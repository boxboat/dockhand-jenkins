package com.boxboat.jenkins.library.credentials.vault

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config

public class VaultStringCredential extends VaultBaseCredential {

    public String stringKey

    static class Params extends BaseConfig<Params> implements Serializable {
        String variable
    }

    def withCredentials(Params params, Closure closure) {
        params.variable = params.variable ?: "STRING"
        def stringContent = getSecret(stringKey)

        def maskedPairs = [
                [
                        var     : params.variable,
                        password: stringContent,
                ],
        ]
        def maskedEnvList = maskedPairs.collect { "${it.var}=${it.password}" }
        Config.pipeline.wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: maskedPairs]) {
            Config.pipeline.withEnv(maskedEnvList) {
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
