package com.boxboat.jenkins.library.git

import com.boxboat.jenkins.library.config.BaseConfig

class GitConfig extends BaseConfig<GitConfig> implements Serializable {

    String buildVersionsUrl

    String buildVersionsLockableResource

    String email

    String credential

    String remotePathRegex

    String remoteUrlReplace

    String branchUrlReplace

    String commitUrlReplace

    Map<String, GitConfig> gitAlternateMap

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
}
