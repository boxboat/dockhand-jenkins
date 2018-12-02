package com.boxboat.jenkins.library.config

import com.boxboat.jenkins.library.docker.Image
import com.boxboat.jenkins.library.docker.Registry
import com.boxboat.jenkins.library.event.EventRegistryKey
import com.boxboat.jenkins.library.trigger.Trigger

class CommonConfigBase<T> extends BaseConfig<T> {

    String defaultBranch

    String event

    List<EventRegistryKey> eventRegistryKeys

    List<Image> images

    String notifyTargetKeySuccess

    String notifyTargetKeyFailure

    List<Trigger> triggers

    String vaultKey

    List<Registry> getEventRegistries(String event) {
        List<Registry> registries = []
        eventRegistryKeys?.each { EventRegistryKey eventRegistryKey ->
//            def matcher = event =~ eventRegistry.event
//            if (matcher.hasGroup()) {
//                registries.add(GlobalConfig.config.getRegistry(eventRegistry.registryKey))
//            }
            if (event == eventRegistryKey.event) {
                registries.add(GlobalConfig.config.getRegistry(eventRegistryKey.registryKey))
            }
        }
        return registries
    }

}
