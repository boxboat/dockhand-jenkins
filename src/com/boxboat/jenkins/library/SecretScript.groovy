package com.boxboat.jenkins.library

class SecretScript {

    static replace(steps, Vault vault, List<String> globs) {
        steps.withCredentials([steps.string(
                credentialsId: vault.credentials,
                variable: 'VAULT_TOKEN',
        )]) {
            steps.sh """
                export VAULT_ADDR="${vault.url}"
                ./sharedLibraryScripts/secret-replace.sh "${globs.join('" "')}"
            """
        }
    }

    static file(steps, Vault vault, List<String> vaultKeys, String outFile, String format, boolean append) {
        if (format == "") {
            if (outFile.toLowerCase().endsWith(".yml") || outFile.toLowerCase().endsWith(".yaml")) {
                format = "yaml"
            } else {
                format = "env"
            }
        }
        steps.withCredentials([steps.string(
                credentialsId: vault.credentials,
                variable: 'VAULT_TOKEN',
        )]) {
            steps.sh """
                export VAULT_ADDR="${vault.url}"
                ./sharedLibraryScripts/secret-env.sh ${append ? "--append" : ""} --format "${format}" --output "${outFile}" "${vaultKeys.join('" "')}"
            """
        }
    }

}
