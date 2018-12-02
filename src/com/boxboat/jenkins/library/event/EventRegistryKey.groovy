package com.boxboat.jenkins.library.event

import com.boxboat.jenkins.library.config.BaseConfig

class EventRegistryKey extends BaseConfig<EventRegistryKey> {

    String event

    Object notify

    String registryKey

}
