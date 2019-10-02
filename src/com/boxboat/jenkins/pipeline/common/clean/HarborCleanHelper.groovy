package com.boxboat.jenkins.pipeline.common.clean

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.docker.HarborRegistryClean

class HarborCleanHelper implements Serializable {

    static class HarborCleanParams extends BaseConfig<HarborCleanParams> implements Serializable {
        boolean dryRun = false
        int retentionDays = 15
        List<String> registryKeys
    }

    static clean(Map params) {
        def paramsObj = new HarborCleanParams().newFromObject(params)
        if (!paramsObj.registryKeys) {
            Config.pipeline.error "'registryKeys' is required"
        }

        HarborRegistryClean harborClean = new HarborRegistryClean(
                dryRun: paramsObj.dryRun,
                retentionDays: paramsObj.retentionDays,
                registryKeys: paramsObj.registryKeys,
        )
        harborClean.clean()
    }

}
