package com.boxboat.jenkins.library.docker

import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.credentials.vault.VaultUsernamePasswordCredential

class Registry extends BaseConfig<Registry> implements Serializable {

    String scheme

    String host

    String namespace

    Object credential

    String imageUrlReplace

    static class Params extends BaseConfig<Params> implements Serializable {
        String usernameVariable
        String passwordVariable
    }

    def getRegistryImageUrl(String path, String tag = "latest") {
        if (!imageUrlReplace || !path) {
            return ""
        }
        return imageUrlReplace.replaceFirst(/(?i)\{\{\s+path\s+\}\}/, path).replaceFirst(/(?i)\{\{\s+tag\s+\}\}/, tag)
    }

    def getRegistryUrl() {
        return "${scheme}://${host}"
    }

    void makeDockerConfig(String username, String password, String dir = "~/.docker") {
        def fileName = "${dir}/config.json"
        def reg = ["auths": [
                "${getRegistryUrl()}": [
                        "auth": "${username}:${password}".bytes.encodeBase64().toString()
                ]]]

        if (Config.pipeline.fileExists(fileName)) {
            def data = Config.pipeline.readJSON(file: fileName)
            reg['auths'] = data['auths'] + reg['auths']
        }
        Config.pipeline.writeJSON(file: fileName, json: reg)
    }

    def withCredentials(Params params, closure) {
        params.usernameVariable = params.usernameVariable ?: "REGISTRY_USERNAME"
        params.passwordVariable = params.passwordVariable ?: "REGISTRY_PASSWORD"

        def credClosure = {
            Utils.withTmpDir("DOCKER_CONFIG") {
                def username = Config.pipeline.env[params.usernameVariable]
                def password = Config.pipeline.env[params.passwordVariable]

                if (Utils.hasCmd('docker')) {
                    Config.pipeline.sh """
                        echo -n "\${REGISTRY_PASSWORD}" | docker login -u "\${REGISTRY_USERNAME}" --password-stdin "${
                        getRegistryUrl()
                    }"
                    """
                } else {
                    makeDockerConfig(username, password, Config.pipeline.env["DOCKER_CONFIG"])
                }
                closure()
            }
        }

        if (credential instanceof VaultUsernamePasswordCredential) {
            credential.withCredentials([
                "usernameVariable": params.usernameVariable,
                "passwordVariable": params.passwordVariable
            ]) {
                credClosure()
            }
        } else {
            Config.pipeline.withCredentials([Config.pipeline.usernamePassword(
                    credentialsId: credential,
                    usernameVariable: params.usernameVariable,
                    passwordVariable: params.passwordVariable,
            )]) {
                credClosure()
            }
        }
    }

    def withCredentials(Map paramsMap, closure) {
        withCredentials(new Params().newFromObject(paramsMap), closure)
    }

    def withCredentials(Closure closure) {
        withCredentials(new Params(), closure)
    }
}
