package com.boxboat.jenkins.pipeline.clean

import com.boxboat.jenkins.pipeline.BoxBase
import com.boxboat.jenkins.library.config.BuildConfig
import com.boxboat.jenkins.library.docker.RegistryClean

class BoxClean extends BoxBase<BuildConfig> implements Serializable {
    BoxClean(Map config = [:]) {
        super(config)
    }

    @Override
    protected String configKey() {
        return "clean"
    }

    def init() {
        super.init()
    }
    def cleanRegistry(def dryRun, def registryName = "default"){
        RegistryClean registryClean = new RegistryClean(dryRun)
        registryClean.cleanRegistry(registryName)
    }

}
