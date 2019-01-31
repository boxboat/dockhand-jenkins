package com.boxboat.jenkins.pipeline.common.vault

import com.boxboat.jenkins.library.LibraryScript
import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.vault.Vault
import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.CommonConfigBase

class VaultSecretScriptHelper implements Serializable {

    private static CommonConfigBase repoConfig() {
        return Config.<CommonConfigBase>castRepo()
    }

    static class ReplaceParams extends BaseConfig implements Serializable {
        String vaultKey
        Map<String, String> env
        List<String> globs
    }

    static replace(Map<String, Object> paramsMap) {
        def params = (new ReplaceParams()).newFromObject(paramsMap)

        if (!params.globs) {
            Config.pipeline.error "'globs' is required"
        }
        String vaultKey = params.vaultKey ?: repoConfig().vaultKey
        if (!vaultKey) {
            Config.pipeline.error "'vaultKey' is required"
        }
        Vault vault = Config.global.getVault(vaultKey)

        vault.withCredentials {
            def envStr = params.env ? params.env.collect { k, v ->
                return "--env \"${k}=${v}\""
            }.join(" ") : ""

            Config.pipeline.sh """
                set +x
                export VAULT_ADDR="${vault.url}"
                . ${LibraryScript.run("vault-login.sh")}
                set -x
                ${LibraryScript.run("secret-replace.sh")} \\
                    ${envStr} \\
                    "${params.globs.join('" "')}"
            """
        }
    }

    static class FileParams extends BaseConfig implements Serializable {
        Boolean base64
        String format
        String outFile
        List<String> yamlPath
        String vaultKey
        List<String> vaultPaths
    }

    static file(Map<String, Object> paramsMap) {
        def params = (new FileParams()).newFromObject(paramsMap)

        if (!params.outFile) {
            Config.pipeline.error "'outFile' is required"
        }
        if (!params.vaultPaths) {
            Config.pipeline.error "'vaultPaths' is required"
        }
        if (!params.format) {
            params.format = Utils.fileFormatDetect(params.outFile)
        }
        params.format = Utils.fileFormatNormalize(params.format)
        if (params.format != "yaml" && params.format != "env") {
            Config.pipeline.error "'format' is required and must be either 'yaml' or 'env'"
        }

        String vaultKey = params.vaultKey ?: repoConfig().vaultKey
        if (!vaultKey) {
            Config.pipeline.error "'vaultKey' is required"
        }
        Vault vault = Config.global.getVault(vaultKey)

        String base64Script = ""
        if (params.base64) {
            base64Script = """
                base64 -w 0 "${params.outFile}" > "${params.outFile}.b64"
                mv "${params.outFile}.b64" "${params.outFile}"
            """
        }

        vault.withCredentials {
            Config.pipeline.sh """
                set +x
                export VAULT_ADDR="${vault.url}"
                . ${LibraryScript.run("vault-login.sh")}
                set -x
                ${LibraryScript.run("secret-env.sh")} \\
                    --format "${params.format}" \\
                    --kv-version "${vault.kvVersion}" \\
                    --output "${params.outFile}" \\
                    "${params.vaultPaths.join('" "')}"
                ${base64Script}
                ${Utils.yamlPathScript(params.yamlPath, params.outFile, params.format)}
            """
        }
    }

}
