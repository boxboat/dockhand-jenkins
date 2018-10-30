package com.boxboat.jenkins.library

class ServerConfig {

    static final gitEmail = 'jenkins@ORG.com'
    static final String gitCredentials = 'jenkins-svc-key'
    static final String registryScheme = "https"
    static final String registryCredentials = "dtr"
    static final Map<String, String> registryMap = [
            "dtr": "dtr.boxboat.com",
    ]

    static final buildVersionsGitRemoteUrl = "ssh://git@github.com/ORG/build-versions.git"

    static gitRemotePath(String url) {
        def matcher = url =~ /github\.com\/(.*)\.git$/
        return matcher.hasGroup() ? matcher[0][1] : null
    }

    static gitRemoteUrl(String path) {
        return "git@github.com/${path}.git"
    }

}
