package com.boxboat.jenkins.pipeline.common.clean

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.docker.ArtifactoryClean
import com.boxboat.jenkins.library.docker.ArtifactoryCleanRegistry

class ArtifactoryCleanHelper implements Serializable {

    static class ArtifactoryCleanParams extends BaseConfig<ArtifactoryCleanParams> implements Serializable {
        boolean dryRun = false
        int retentionDays = 15
        List<ArtifactoryCleanRegistry> artifactoryCleanRegistries
    }

    static String clean(Map params) {
        def paramsObj = new ArtifactoryCleanParams().newFromObject(params)
        if (!paramsObj.artifactoryCleanRegistries) {
            Config.pipeline.error "'artifactoryCleanRegistries' is required"
        }

        def artifactoryClean = new ArtifactoryClean(
                dryRun: paramsObj.dryRun,
                retentionDays: paramsObj.retentionDays,
                artifactoryCleanRegistries: paramsObj.artifactoryCleanRegistries,
        )
        return artifactoryClean.clean()
    }

}
