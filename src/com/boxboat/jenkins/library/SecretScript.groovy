package com.boxboat.jenkins.library

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.CommonConfigBase
import com.boxboat.jenkins.library.config.GlobalConfig

class SecretScript {

    static class ReplaceParams extends BaseConfig<ReplaceParams> {
        String vaultKey
        Map<String, String> env
        List<String> globs
    }

    static replace(steps, Map<String, Object> paramsMap, CommonConfigBase config) {
        def params = (new ReplaceParams()).newFromObject(paramsMap)

        if (!params.globs) {
            steps.error "'globs' is required"
        }
        String vaultKey = params.vaultKey ?: config.vaultKey
        if (!vaultKey) {
            steps.error "'vaultKey' is required"
        }
        Vault vault = GlobalConfig.config.getVault(vaultKey)

        steps.withCredentials(vault.getCredentials(steps)) {
            def envStr = params.env ? params.env.collect { k, v ->
                return "--env \"${k}=${v}\""
            }.join(" ") : ""

            steps.sh """
                set +x
                export VAULT_ADDR="${vault.url}"
                . ${LibraryScript.run(steps, "vault-login.sh")}
                set -x
                ${LibraryScript.run(steps, "secret-replace.sh")} \\
                    ${envStr} \\
                    "${params.globs.join('" "')}"
            """
        }
    }

    static class FileParams extends BaseConfig<FileParams> {
        Boolean base64
        String format
        String outFile
        List<String> yamlPath
        String vaultKey
        List<String> vaultPaths
    }

    static file(steps, Map<String, Object> paramsMap, CommonConfigBase config) {
        def params = (new FileParams()).newFromObject(paramsMap)

        if (!params.outFile) {
            steps.error "'outFile' is required"
        }
        if (!params.vaultPaths) {
            steps.error "'vaultPaths' is required"
        }
        if (!params.format) {
            params.format = Utils.fileFormatDetect(params.outFile)
        }
        params.format = Utils.fileFormatNormalize(params.format)
        if (params.format != "yaml" && params.format != "env") {
            steps.error "'format' is required and must be either 'yaml' or 'env'"
        }

        String vaultKey = params.vaultKey ?: config.vaultKey
        if (!vaultKey) {
            steps.error "'vaultKey' is required"
        }
        Vault vault = GlobalConfig.config.getVault(vaultKey)

        String base64Script = ""
        if (params.base64) {
            base64Script = """
                base64 -w 0 "${params.outFile}" > "${params.outFile}.b64"
                mv "${params.outFile}.b64" "${params.outFile}"
            """
        }

        steps.withCredentials(vault.getCredentials(steps)) {
            steps.sh """
                set +x
                export VAULT_ADDR="${vault.url}"
                . ${LibraryScript.run(steps, "vault-login.sh")}
                set -x
                ${LibraryScript.run(steps, "secret-env.sh")} \\
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
