package com.boxboat.jenkins.library.deploy

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.docker.Image
import com.boxboat.jenkins.library.trigger.ITriggerable

class Deployment extends BaseConfig<Deployment> implements Serializable, ITriggerable {

    String environmentKey

    String event

    String eventRegex

    String eventFallback

    List<Image> imageOverrides

    Boolean trigger

    String triggerBranch
}
