package com.boxboat.jenkins.library.git

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.credentials.vault.VaultFileCredential

class GitConfig extends BaseConfig<GitConfig> implements Serializable {

    String buildVersionsUrl

    String buildVersionsLockableResource

    String email

    Object credential

    String remotePathRegex

    String remoteUrlReplace

    String branchUrlReplace

    String commitUrlReplace

    Map<String, GitConfig> gitAlternateMap

    static class Params extends BaseConfig<Params> implements Serializable {
        String keyFileVariable
        String usernameVariable
    }

    GitConfig getGitConfig(String key) {
        def gitConfig = this
        if (key) {
            gitConfig = gitAlternateMap.get(key)
        }
        if (!gitConfig) {
            throw new Exception("git.gitAlternateMap entry '${key}' does not exist in config file")
        }
        return gitConfig
    }

    String getCommitUrl(String path, String hash) {
        if (!commitUrlReplace || !path || !hash) {
            return ""
        }
        return replacePath(commitUrlReplace, path).replaceFirst(/(?i)\{\{\s+hash\s+\}\}/, hash)
    }

    String getBranchUrl(String path, String branch) {
        if (!branchUrlReplace || !path || !branch) {
            return ""
        }
        return replacePath(branchUrlReplace, path).replaceFirst(/(?i)\{\{\s+branch\s+\}\}/, branch)
    }

    static String replacePath(String base, String path) {
        return base.replaceFirst(/(?i)\{\{\s+path\s+\}\}/, path)
    }

    def withCredentials(Params params, closure) {
        params.keyFileVariable = params.keyFileVariable ?: "SSH_KEY"
        params.usernameVariable = params.usernameVariable ?: "USERNAME"

        if (credential instanceof VaultFileCredential) {
            credential.withCredentials(['variable': params.keyFileVariable]){
                closure()
            }
        }else{
            Config.pipeline.withCredentials([Config.pipeline.sshUserPrivateKey(
                credentialsId: credential,
                keyFileVariable: params.keyFileVariable,
                usernameVariable: params.usernameVariable
            )]) {
                closure()
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
