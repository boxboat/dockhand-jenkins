package com.boxboat.jenkins.library.git

import com.boxboat.jenkins.library.config.BaseConfig

class GitConfig extends BaseConfig<GitConfig> {

    String buildVersionsUrl

    String email

    String credential

    String remotePathRegex

    String remoteUrlReplace

    String getRemotePath(String url) {
        def matcher = url =~ remotePathRegex
        return matcher.hasGroup() ? matcher[0][1] : null
    }

    String getRemoteUrl(String path) {
        return remoteUrlReplace.replaceFirst(/(?i)\{\{\s+path\s+\}\}/, path)
    }

}
