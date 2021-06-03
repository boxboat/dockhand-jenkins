package com.boxboat.jenkins.pipeline.common.clean

import com.boxboat.jenkins.library.buildVersions.GitBuildVersionsClean
import com.boxboat.jenkins.library.config.BaseConfig

class GitBuildVersionsCleanHelper implements Serializable {

    static class GitBuildVersionsCleanParams extends BaseConfig<GitBuildVersionsCleanParams> implements Serializable {
        boolean dryRun = false
    }

    static String clean(Map params) {
        def paramsObj = new GitBuildVersionsCleanParams().newFromObject(params)
        def buildVersionsClean = new GitBuildVersionsClean(
                dryRun: paramsObj.dryRun,
        )
        return buildVersionsClean.clean()
    }

}
