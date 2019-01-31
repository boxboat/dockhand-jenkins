package com.boxboat.jenkins.library.config

import com.boxboat.jenkins.library.deploy.Deployment
import com.boxboat.jenkins.library.docker.Image

class DeployConfig extends CommonConfigBase implements Serializable {

    String deploymentKey

    String deployTargetKey

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
