package com.boxboat.jenkins.library.event

import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.docker.Registry

class EventConfig implements Serializable {

    public Map<String, List<String>> registryEventMap = [:]

    List<Registry> getEventRegistries(String event) {
        List<Registry> registries = []
        registryEventMap.each { registryStr, eventList ->
            eventList.each { eventRe ->
                def matcher = event =~ eventRe
                if (matcher.hasGroup() && matcher.size() > 0) {
                    registries.add(Config.global.getRegistry(registryStr))
                }
            }
        }
        return registries
    }

}
