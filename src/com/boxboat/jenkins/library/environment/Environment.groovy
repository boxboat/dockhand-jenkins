package com.boxboat.jenkins.library.environment

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.deployTarget.IDeployTarget

class Environment extends BaseConfig<Environment> implements Serializable {

    String name

    String deployTargetKey

    List<Environment> replicaEnvironments

    List<Environment> allEnvironments() {
        return [this] + (replicaEnvironments ?: [])
    }

    IDeployTarget getDeployTarget() {
        return Config.global.getDeployTarget(deployTargetKey)
    }

    void withCredentials(closure) {
        getDeployTarget().withCredentials(closure)
    }

}
