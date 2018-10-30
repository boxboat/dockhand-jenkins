package com.boxboat.jenkins.library

class Utils {

    static String cleanEvent(String event) {
        if (event == null) {
            return null
        }
        def eventSplit = event.split("/", 2)
        if (eventSplit.size() > 1) {
            eventSplit[1] = cleanTag(eventSplit[1])
        }
        return eventSplit.join("/")
    }

    static String cleanTag(String tag) {
        if (tag == null) {
            return null
        }
        return tag.replaceAll(/[^a-zA-Z0-9\-.]/, '-').toLowerCase()
    }

    static String alphaNumericDashLower(String value) {
        if (value == null) {
            return null
        }
        return value.replaceAll(/[^a-zA-Z0-9\-]/, '-').toLowerCase()
    }

    static String alphaNumericUnderscoreLower(String value) {
        if (value == null) {
            return null
        }
        return value.replaceAll(/[^a-zA-Z0-9_]/, '_').toLowerCase()
    }

}
