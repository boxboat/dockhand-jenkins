package com.boxboat.jenkins.library.config

import com.boxboat.jenkins.library.buildVersions.GitBuildVersions
import com.boxboat.jenkins.library.git.GitAccount
import com.boxboat.jenkins.library.git.GitConfig

class Config implements Serializable {

    static String baseDir = "./"

    static String scmDir = "./"

    static GlobalConfig global

    static CommonConfigBase repo

    static Object pipeline

    static <T extends CommonConfigBase> T castRepo() {
        return (T) repo
    }

    static GitAccount gitAccount = new GitAccount()

    private static GitBuildVersions _buildVersions

    static GitBuildVersions getBuildVersions() {
        if (!_buildVersions) {
            _buildVersions = new GitBuildVersions()
            _buildVersions.checkout()
        }
        return _buildVersions
    }

    static GitConfig getGitSelected() {
        return global.git.getGitConfig(repo.gitAlternateKey)
    }

    static String getGitRemotePath(String url) {
        return global.getGitRemotePath(repo.gitAlternateKey, url)
    }

    static String getGitRemoteUrl(String path) {
        return global.getGitRemoteUrl(path)
    }

    static resetBuildVersions() {
        _buildVersions = null
    }

}
