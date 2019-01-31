package com.boxboat.jenkins.library.config

class RepoConfig extends BaseConfig implements Serializable {

    BuildConfig build

    CommonConfig common

    DeployConfig deploy

    PromoteConfig promote

}
