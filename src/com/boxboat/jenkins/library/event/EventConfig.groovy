package com.boxboat.jenkins.library.event

import com.boxboat.jenkins.library.config.GlobalConfig
import com.boxboat.jenkins.library.docker.Registry

class EventConfig {

    public Map<String, List<String>> registryEventMap = [:]

    List<Registry> getEventRegistries(String event) {
        List<Registry> registries = []
        registryEventMap.each { registryStr, eventList ->
            eventList.each { eventRe ->
                def matcher = event =~ eventRe
                if (matcher.hasGroup()) {
                    registries.add(GlobalConfig.config.getRegistry(registryStr))
                }
            }
        }
        return registries
    }

}
