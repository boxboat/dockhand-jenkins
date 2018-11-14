package com.boxboat.jenkins.library

class SecretScript {

    static replace(steps, Vault vault, List<String> globs, Map<String, String> env) {
        steps.withCredentials(vault.getCredentials(steps)) {
            def envStr = env.collect { k, v ->
                return "--env \"${k}=${v}\""
            }.join(" ")

            steps.sh """
                export VAULT_ADDR="${vault.url}"
                source ${LibraryScrpt.run(steps, "vault-login.sh")}
                ${LibraryScript.run(steps, "secret-replace.sh")} \\
                    ${envStr} \\
                    "${globs.join('" "')}"
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
        steps.withCredentials(vault.getCredentials(steps)) {
            steps.sh """
                export VAULT_ADDR="${vault.url}"
                source ${LibraryScrpt.run(steps, "vault-login.sh")}
                ${LibraryScript.run(steps, "secret-env.sh")} \\
                    ${append ? "--append" : ""} \\
                    --format "${format}" \\
                    --kv-version "${vault.kvVersion}" \\
                    --output "${outFile}" \\
                    "${vaultKeys.join('" "')}"
            """
        }
    }

}
