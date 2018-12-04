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

    static replace(Map<String, Object> paramsMap, CommonConfigBase config) {
        def params = (new ReplaceParams()).newFromObject(paramsMap)

        if (!params.globs) {
            GlobalConfig.pipeline.error "'globs' is required"
        }
        String vaultKey = params.vaultKey ?: config.vaultKey
        if (!vaultKey) {
            GlobalConfig.pipeline.error "'vaultKey' is required"
        }
        Vault vault = GlobalConfig.config.getVault(vaultKey)

        GlobalConfig.pipeline.withCredentials(vault.getCredentials()) {
            def envStr = params.env ? params.env.collect { k, v ->
                return "--env \"${k}=${v}\""
            }.join(" ") : ""

            GlobalConfig.pipeline.sh """
                set +x
                export VAULT_ADDR="${vault.url}"
                . ${LibraryScript.run( "vault-login.sh")}
                set -x
                ${LibraryScript.run( "secret-replace.sh")} \\
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

    static file(Map<String, Object> paramsMap, CommonConfigBase config) {
        def params = (new FileParams()).newFromObject(paramsMap)

        if (!params.outFile) {
            GlobalConfig.pipeline.error "'outFile' is required"
        }
        if (!params.vaultPaths) {
            GlobalConfig.pipeline.error "'vaultPaths' is required"
        }
        if (!params.format) {
            params.format = Utils.fileFormatDetect(params.outFile)
        }
        params.format = Utils.fileFormatNormalize(params.format)
        if (params.format != "yaml" && params.format != "env") {
            GlobalConfig.pipeline.error "'format' is required and must be either 'yaml' or 'env'"
        }

        String vaultKey = params.vaultKey ?: config.vaultKey
        if (!vaultKey) {
            GlobalConfig.pipeline.error "'vaultKey' is required"
        }
        Vault vault = GlobalConfig.config.getVault(vaultKey)

        String base64Script = ""
        if (params.base64) {
            base64Script = """
                base64 -w 0 "${params.outFile}" > "${params.outFile}.b64"
                mv "${params.outFile}.b64" "${params.outFile}"
            """
        }

        GlobalConfig.pipeline.withCredentials(vault.getCredentials()) {
            GlobalConfig.pipeline.sh """
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
