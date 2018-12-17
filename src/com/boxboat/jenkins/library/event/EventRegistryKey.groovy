package com.boxboat.jenkins.library.event

import com.boxboat.jenkins.library.config.BaseConfig

class EventRegistryKey extends BaseConfig<EventRegistryKey> implements Serializable {

    String event

    String eventRegex

    String registryKey

}
