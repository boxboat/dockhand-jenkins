package com.boxboat.jenkins.pipeline.common

import com.boxboat.jenkins.library.config.CommonConfig
import com.boxboat.jenkins.pipeline.BoxBase

class BoxCommon extends BoxBase<CommonConfig> {

    BoxCommon(Map config) {
        super(config)
    }

    static def create(Map config) {
        def common = new BoxCommon(config)
        return common
    }

    @Override
    protected String configKey() {
        return "common"
    }

}
