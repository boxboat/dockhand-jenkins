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

    String getRemotePath(String url) {
        if (!remotePathRegex) {
            return ""
        }
        def matcher = url =~ remotePathRegex
        return matcher.hasGroup() && matcher.size() > 0 ? matcher[0][1] : null
    }

    String getRemoteUrl(String path) {
        if (!remoteUrlReplace || !path) {
            return ""
        }
        return replacePath(remoteUrlReplace, path)
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

    private String replacePath(String base, String path){
        return base.replaceFirst(/(?i)\{\{\s+path\s+\}\}/, path)
    }
}
