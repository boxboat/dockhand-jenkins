package com.boxboat.jenkins.pipeline.common.dockcmd

import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.aws.AwsProfile
import com.boxboat.jenkins.library.vault.Vault

class DockcmdGetSecrets implements Serializable {

    public String awsProfileKey

    public String vaultKey

    public String directory = "."

    public String[] files = []

    Map<String, Object> options = [:]

    public parseAwsSecrets(Map<String, Object> additionalOptions = [:]) {
        if (!awsProfileKey) {
            Config.pipeline.error "'awsProfileKey' is required"
        }
        AwsProfile aws = Config.global.getAwsProfile(awsProfileKey)
        aws.withCredentials {
            Config.pipeline.sh parseAwsSecretsScript(aws.region, additionalOptions)
        }
    }

    public parseAwsSecretsScript(String region, Map<String, Object> additionalOptions = [:]) {
        def combinedOptions = combineOptions(options, additionalOptions)
        return """
            dockcmd_current_dir=\$(pwd)
            cd "${directory}"
            dockcmd aws get-secrets --region "${region}" ${optionsString(combinedOptions)} ${files.join('" "')}
            cd "\$dockcmd_current_dir"
        """

    }

    public parseVaultSecrets(Map<String, Object> additionalOptions = [:]) {
        if (!vaultKey) {
            Config.pipeline.error "'vaultKey' is required"
        }
        Vault vault = Config.global.getVault(vaultKey)
        vault.withCredentials {
            Config.pipeline.sh parseVaultSecretsScript(vault.url, additionalOptions)
        }
    }

    public parseVaultSecretsScript(String vaultUrl, Map<String, Object> additionalOptions = [:]) {
        def combinedOptions = combineOptions(options, additionalOptions)
        return """
            dockcmd_current_dir=\$(pwd)
            cd "${directory}"
            dockcmd vault get-secrets --vault-addr "${vaultUrl}" ${optionsString(combinedOptions)}  ${files.join('" "')}
            cd "\$dockcmd_current_dir"
        """

    }

    private static Map<String, List<String>> combineOptions(Map<String, Object> options1, Map<String, Object> options2) {
        Map<String, List<String>> combinedOptions = [:]
        def combineCl = { String k, v ->
            if (!(v instanceof List)) {
                v = [v]
            }
            combinedOptions[k] = combinedOptions.get(k, []) + v
        }
        options1.each(combineCl)
        options2.each(combineCl)
        return combinedOptions
    }

    private static optionsString(Map<String, Object> allOptions) {
        return allOptions.collect { k, v ->
            def optionSwitch = k.length() == 1 ? "-${k}" : "--${k}"
            if (!(v instanceof List)) {
                v = [v]
            }
            return v.collect { optionValue ->
                if (optionValue instanceof Boolean && optionValue) {
                    return "$optionSwitch"
                }
                optionValue = optionValue.toString()
                return "$optionSwitch \"${optionValue.replace('"', '\\"')}\""
            }.join(" ")
        }.join(" ")
    }
}
