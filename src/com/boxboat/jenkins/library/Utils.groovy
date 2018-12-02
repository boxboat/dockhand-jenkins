package com.boxboat.jenkins.library

import java.lang.reflect.Modifier

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

    static String yamlPathScript(List<String> yamlPath, String fileName, String fileFormat) {
        if (!yamlPath) {
            return ""
        }
        def script = ""
        def stringContents = fileFormatNormalize(fileFormat) == "yaml" ? "" : " |"
        yamlPath.reverseEach { path ->
            script += """
                sed -i 's/^/  /g' "${fileName}"
                sed -i '1s/^/${path}:${stringContents}\\n/' "${fileName}"
            """
            stringContents = ""
        }
        return script
    }

    static String fileFormatDetect(String fileName) {
        fileName = fileName?.toLowerCase()
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            return "yaml"
        } else if (fileName.endsWith(".env")) {
            return "env"
        }
        return ""
    }

    static String fileFormatNormalize(String format) {
        format = format?.toLowerCase()
        if (format == "yml") {
            format = "yaml"
        }
        return format
    }

}
