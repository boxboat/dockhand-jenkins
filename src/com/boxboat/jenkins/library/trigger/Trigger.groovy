package com.boxboat.jenkins.library.trigger

import com.boxboat.jenkins.library.config.BaseConfig

class Trigger extends BaseConfig<Trigger> implements Serializable {

    String imagePath

    String event

    Map<String, String> params

}
