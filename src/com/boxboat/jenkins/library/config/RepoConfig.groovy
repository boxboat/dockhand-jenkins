package com.boxboat.jenkins.library.config

class RepoConfig extends BaseConfig<RepoConfig> implements Serializable {

    BuildConfig build

    CommonConfig common

    DeployConfig deploy

    PromoteConfig promote

}
