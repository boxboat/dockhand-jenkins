package com.boxboat.jenkins.library

class SecretScript {

    static replace(steps, List<String> globs) {
        steps.withCredentials([steps.string(
                credentialsId: ServerConfig.vaultCredentials,
                variable: 'VAULT_TOKEN',
        )]) {
            steps.sh """
                export VAULT_ADDR="${ServerConfig.vaultUrl}"
                ./sharedLibraryScripts/secret-replace.sh "${globs.join('" "')}"
            """
        }
    }

    static file(steps, List<String> vaultKeys, String outFile, String format, boolean append) {
        if (format == "") {
            if (outFile.toLowerCase().endsWith(".yml") || outFile.toLowerCase().endsWith(".yaml")) {
                format = "yaml"
            } else {
                format = "env"
            }
        }
        steps.withCredentials([steps.string(
                credentialsId: ServerConfig.vaultCredentials,
                variable: 'VAULT_TOKEN',
        )]) {
            steps.sh """
                export VAULT_ADDR="${ServerConfig.vaultUrl}"
                ./sharedLibraryScripts/secret-env.sh ${append ? "--append" : ""} --format "${format}" --output "${outFile}" "${vaultKeys.join('" "')}"
            """
        }
    }

}
