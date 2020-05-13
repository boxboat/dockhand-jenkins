package com.boxboat.jenkins.library

import com.boxboat.jenkins.library.config.Config

import java.nio.file.Paths

class Utils implements Serializable {

    static String cleanBranch(String branch) {
        if (branch == null) {
            return null
        }
        return branch.replaceAll(/[^a-zA-Z0-9\-.\/]/, '')
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

    static String trimSlash(String value) {
        return value.replaceAll(/(^\/+|\/+$)/, "")
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

    static String generateUUIDString() {
        return UUID.randomUUID().toString()
    }

    static String makeTmpFile() {
        String tmpFile = "./tmp-${generateUUIDString()}"
        Config.pipeline.sh """
            set +x
            touch ${tmpFile}
        """
        return tmpFile
    }

    static void withTmpFile(String fileVariable = "TARGET_FILE", Closure closure) {
        def file = makeTmpFile()
        try {
            Config.pipeline.withEnv(["${fileVariable}=${file}"]) {
                closure()
            }
        } finally {
            Config.pipeline.sh """
                set +x
                rm -f ${file}
            """
        }
    }

    /**
     *  Make a temp dir in the workspace
     *    mktemp -d may not work with pipeline steps like write() or writeJSON()
     *    because it is outside Jenkin's current working directory
     **/
    static String makeTmpDir() {
        String tmpDir = "./tmp-${generateUUIDString()}"
        Config.pipeline.sh """
            set +x
            mkdir -p "${tmpDir}"
        """.trim()
        return tmpDir
    }

    static void withTmpDir(String dirVariable = "TARGET_DIR", Closure closure) {
        def dir = makeTmpDir()
        try {
            Config.pipeline.withEnv(["${dirVariable}=${dir}"]) {
                closure()
            }
        } finally {
            Config.pipeline.sh """
                set +x
                rm -rf ${dir}
            """
        }
    }

    static boolean hasCmd(String cmd) {
        def exists = true
        try {
            Config.pipeline.sh """
                set +x
                which ${cmd}
            """
        } catch (Exception ex) {
            exists = false
        }
        return exists
    }

    static String buildTagCommit(String tag) {
        if (tag.startsWith("build-")) {
            return tag.substring("build-".length())
        }
        return null
    }

    static boolean isImageTagEvent(String event) {
        return event.toLowerCase().startsWith("imagetag/")
    }

    static String imageTagFromEvent(String event) {
        if (isImageTagEvent(event)) {
            return event.substring("imagetag/".length())
        }
        return null
    }

    static resultOrTest(result, test) {
        if (Config.pipeline.env.GRADLE_TEST_ENV == "true") {
            return test
        }
        return result
    }

    static String toAbsolutePath(String relativePath) {
        return Paths.get(Config.pipeline.pwd(), relativePath).normalize().toAbsolutePath().toString()
    }

}
