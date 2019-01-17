package com.boxboat.jenkins.pipeline.common

import com.boxboat.jenkins.library.config.CommonConfig
import com.boxboat.jenkins.pipeline.BoxBase

class BoxCommon extends BoxBase<CommonConfig> implements Serializable {

    BoxCommon(Map config = [:]) {
        super(config)
        setPropertiesFromMap(config)
    }

    @Override
    protected String configKey() {
        return "common"
    }

}
