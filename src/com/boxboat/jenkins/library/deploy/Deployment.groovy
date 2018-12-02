package com.boxboat.jenkins.library.deploy

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.docker.Image

class Deployment extends BaseConfig<Deployment> {

    String environmentKey

    String event

    String eventFallback

    List<Image> imageOverrides

    Object trigger

}
