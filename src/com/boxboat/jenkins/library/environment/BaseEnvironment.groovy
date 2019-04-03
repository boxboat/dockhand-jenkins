package com.boxboat.jenkins.library.environment

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.deployTarget.IDeployTarget

class BaseEnvironment  extends BaseConfig<BaseEnvironment> implements Serializable {
    String name

    String deployTargetKey

    IDeployTarget getDeployTarget() {
        return Config.global.getDeployTarget(deployTargetKey)
    }

    void withCredentials(closure) {
        getDeployTarget().withCredentials(closure)
    }
}
