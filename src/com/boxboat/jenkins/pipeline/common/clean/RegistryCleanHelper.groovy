package com.boxboat.jenkins.pipeline.clean

import com.boxboat.jenkins.pipeline.BoxBase
import com.boxboat.jenkins.library.config.BuildConfig
import com.boxboat.jenkins.library.docker.DTRClean

class RegistryCleanHelper implements Serializable {
    static clean(def dryRun, List<String> registryKeys){
        DTRClean dtrClean = new DTRClean(dryRun)
        for (registryKey in registryKeys) {
            dtrClean.cleanRegistry(registryKey)
        }
    }
}
