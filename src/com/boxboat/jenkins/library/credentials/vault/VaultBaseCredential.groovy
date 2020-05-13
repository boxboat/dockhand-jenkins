package com.boxboat.jenkins.library.credentials.vault

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.credentials.ICredential
import com.boxboat.jenkins.library.vault.Vault

public abstract class VaultBaseCredential extends BaseConfig<VaultBaseCredential> implements ICredential {

    public String vault

    public String path

    def getSecret(def key){
        Vault v = Config.global.getVault(vault)
        def secret = ""
        v.withCredentials{
            Config.pipeline.withEnv(["VAULT_ADDR=${v.url}"]){
                secret = Config.pipeline.sh(returnStdout: true, script: """
                    printf '{{ (vault "${path}" "${key}") }}' | dockcmd vault get-secrets --input-file -
                """)
            }
        }
        return secret
    }
}
