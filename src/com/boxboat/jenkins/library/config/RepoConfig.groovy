package com.boxboat.jenkins.library.config

class RepoConfig extends BaseConfig<RepoConfig> {

    static RepoConfig create(String yamlStr) {
        def repoConfig = new RepoConfig()
        return repoConfig.newFromYaml(yamlStr)
    }

    BuildConfig build

    CommonConfig common

    DeployConfig deploy

    PromoteConfig promote

}
