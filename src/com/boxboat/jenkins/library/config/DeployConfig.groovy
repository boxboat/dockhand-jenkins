package com.boxboat.jenkins.library.config


import com.boxboat.jenkins.library.deploy.Deployment
import com.boxboat.jenkins.library.docker.Image
import com.boxboat.jenkins.library.environment.Environment

class DeployConfig extends CommonConfigBase<DeployConfig> implements Serializable {

    String deploymentKey

    String deployTargetKey

    List<Environment> replicaEnvironments

    Map<String, Deployment> deploymentMap

    String environmentKey

    List<Image> imageOverrides

    Deployment getDeployment(String key) {
        def deployment = deploymentMap.get(key)
        if (!deployment) {
            throw new Exception("deployment entry '${key}' does not exist in config file")
        }
        return deployment
    }

}
