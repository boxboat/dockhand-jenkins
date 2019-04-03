package com.boxboat.jenkins.library.environment

class Environment extends BaseEnvironment implements Serializable {

    List<BaseEnvironment> replicaEnvironments = []

    List<BaseEnvironment> allEnvironments() {
        return [this as BaseEnvironment] + replicaEnvironments
    }
}
