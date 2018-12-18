package com.boxboat.jenkins.pipeline.clean

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.BuildConfig
import com.boxboat.jenkins.library.docker.DTRClean
import com.boxboat.jenkins.pipeline.BoxBase

class DTRCleanHelper implements Serializable {
    static class DTRCleanParams extends BaseConfig<DTRCleanParams> implements Serializable {
        Boolean dryRun
        Integer retentionDays = 15
    }
    static clean(Map<String, Object> paramsMap, List<String> registryKeys){
        def paramsObj = (new DTRCleanParams()).newFromObject(paramsMap)
        def params = paramsObj.asMap()
        DTRClean dtrClean = new DTRClean(params)
        for (registryKey in registryKeys) {
            dtrClean.cleanRegistry(registryKey)
        }
    }
}
