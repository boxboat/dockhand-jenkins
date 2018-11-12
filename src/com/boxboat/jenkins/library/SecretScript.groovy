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

}
