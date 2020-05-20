package com.boxboat.jenkins.library.credentials.vault

import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.Utils

public class VaultFileCredential extends VaultBaseCredential {

    public String fileKey

    static class Params extends BaseConfig<Params> implements Serializable {
        String variable
    }

    def withCredentials(Params params, Closure closure) {
        params.variable = params.variable ?: "FILE"
        Utils.withTmpFile(params.variable) {
            def fileContent = getSecret(fileKey)
            def file = Config.pipeline.env[params.variable]
            Config.pipeline.writeFile(file: file, text: fileContent, encoding: "Utf8")

            def unmaskedEnvList = [
                    "${params.variable}=${file}"
            ]

            Config.pipeline.withEnv(unmaskedEnvList) {
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
