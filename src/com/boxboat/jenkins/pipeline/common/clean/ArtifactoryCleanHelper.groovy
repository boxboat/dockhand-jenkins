package com.boxboat.jenkins.pipeline.common.clean

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.docker.ArtifactoryClean

class ArtifactoryCleanHelper implements Serializable {

    static class ArtifactoryCleanParams extends BaseConfig<ArtifactoryCleanParams> implements Serializable {
        boolean dryRun = false
        int retentionDays = 15
        List<String> registryKeys
        String dockerRepo
        String dockerRepoPathMatch
    }

    static String clean(Map params) {
        def paramsObj = new ArtifactoryCleanParams().newFromObject(params)
        if (!paramsObj.registryKeys) {
            Config.pipeline.error "'registryKeys' is required"
        }

        def artifactoryClean = new ArtifactoryClean(
                dryRun: paramsObj.dryRun,
                retentionDays: paramsObj.retentionDays,
                registryKeys: paramsObj.registryKeys,
                dockerRepo: paramsObj.dockerRepo,
                dockerRepoPathMatch: paramsObj.dockerRepoPathMatch
        )
        return artifactoryClean.clean()
    }

}
