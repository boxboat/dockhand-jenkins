package com.boxboat.jenkins.library.trigger

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.docker.Image
import org.apache.commons.lang.builder.HashCodeBuilder
@Grab('org.apache.commons:commons-lang3:3.9')
import org.apache.commons.lang3.builder.EqualsBuilder

class Trigger extends BaseConfig<Trigger> implements Serializable {

    String job

    List<String> imagePaths = []

    String event

    String eventRegex

    List<Map> params = []

    boolean triggerEquals(Object o) {
        if (!(o instanceof Trigger)) {
            return false
        }
        Trigger m = (Trigger) o

        return new EqualsBuilder()
                .append(job, m.job)
                .append(event, m.event)
                .append(eventRegex, m.eventRegex)
                .append(params, m.params)
                .equals
    }

    int triggerHashCode() {
        return new HashCodeBuilder(17, 37)
                .append(job)
                .append(event)
                .append(eventRegex)
                .append(params)
                .toHashCode()
    }

    static List<Trigger> merge(List<Trigger> triggers) {
        // make map of trigger hash codes
        Map<Integer, List<Trigger>> triggerListMap = [:]
        triggers.each { trigger ->
            def triggerHashCode = trigger.triggerHashCode()
            if (!triggerListMap[triggerHashCode]) {
                triggerListMap[triggerHashCode] = []
            }
            triggerListMap[triggerHashCode] += trigger
        }

        //iterate map, merging triggers that are equal
        List<Trigger> mergedTriggers = []
        triggerListMap.keySet().toList().each { triggerHashCode ->
            def likeTriggers = triggerListMap[triggerHashCode]
            while (likeTriggers.size() > 0) {
                List<Integer> remove = []
                for (def i = 1; i < likeTriggers.size(); i++) {
                    if (likeTriggers[0].triggerEquals(likeTriggers[i])) {
                        likeTriggers[0].imagePaths += likeTriggers[i].imagePaths
                        remove += i
                    }
                }
                int offset = 0
                remove.each { i ->
                    likeTriggers.removeAt(i - offset)
                    offset++
                }
                likeTriggers[0].imagePaths = likeTriggers[0].imagePaths.toSet().toList()
                mergedTriggers.add(likeTriggers.remove(0))
            }
        }
        return mergedTriggers
    }

    static List<Trigger> matches(List<String> events, List<Image> images, List<Trigger> triggers) {
        events = events.toSet().toList()
        List<String> imagePaths = images.collect { image -> return image.path }.toSet().toList()
        List<Trigger> matchingTriggers = []
        triggers.each { Trigger trigger ->
            trigger.imagePaths = trigger.imagePaths.intersect(imagePaths).toList()
            if (trigger.imagePaths) {
                events.each { event ->
                    def matches = false
                    if (trigger.event) {
                        matches = event == trigger.event
                    } else if (trigger.eventRegex) {
                        def matcher = event =~ trigger.eventRegex
                        matches = matcher.matches()
                    }
                    if (matches) {
                        def matchingTrigger = trigger.copy()
                        matchingTrigger.event = event
                        matchingTrigger.eventRegex = null
                        matchingTriggers.add(matchingTrigger)
                    }
                }
            }
        }
        return matchingTriggers
    }

}
