package com.boxboat.jenkins.pipeline.common.clean

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.docker.DTRClean

class DTRCleanHelper implements Serializable {

    static class DTRCleanParams extends BaseConfig<DTRCleanParams> implements Serializable {
        boolean dryRun = false
        int retentionDays = 15
        List<String> registryKeys
    }

    static clean(Map params) {
        def paramsObj = new DTRCleanParams().newFromObject(params)
        if (!paramsObj.registryKeys) {
            Config.pipeline.error "'registryKeys' is required"
        }

        DTRClean dtrClean = new DTRClean(
                dryRun: paramsObj.dryRun,
                retentionDays: paramsObj.retentionDays,
                registryKeys: paramsObj.registryKeys,
        )
        dtrClean.clean()
    }

}
