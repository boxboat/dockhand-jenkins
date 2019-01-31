package com.boxboat.jenkins.library.deploy

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.docker.Image

class Deployment extends BaseConfig implements Serializable {

    String environmentKey

    String event

    String eventRegex

    String eventFallback

    List<Image> imageOverrides

    Boolean trigger

}
