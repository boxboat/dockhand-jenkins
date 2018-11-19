package com.boxboat.jenkins.pipeline

import static com.boxboat.jenkins.library.Config.Config

class BoxDeployTarget extends BoxDeployBase {

    String deployTarget = ""

    BoxDeployTarget(Map config) {
        super(config)
        config?.each { k, v -> this[k] = v }
    }

    static def createBoxDeployTarget(Map config) {
        return new BoxDeployTarget(config)
    }

    def init() {
        super.init()
        if (!deployTarget) {
            steps.error "'deployTarget' must be set"
        }
    }

    def withCredentials(closure) {
        Config.getDeployTarget(deployTarget).withCredentials(steps, closure)
    }

}
