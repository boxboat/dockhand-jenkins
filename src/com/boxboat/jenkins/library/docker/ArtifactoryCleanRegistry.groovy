package com.boxboat.jenkins.library.docker

import com.boxboat.jenkins.library.config.BaseConfig

class ArtifactoryCleanRegistry extends BaseConfig<ArtifactoryCleanRegistry> implements  Serializable{
    String pathMatch
    String registryKey
}