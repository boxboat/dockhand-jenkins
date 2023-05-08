package com.boxboat.jenkins.library.buildVersions

import com.boxboat.jenkins.library.SemVer
import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.docker.Image
import com.boxboat.jenkins.library.git.GitRepo
import com.boxboat.jenkins.library.trigger.Trigger
import com.boxboat.jenkins.library.yaml.YamlUtils

class GitBuildVersions implements Serializable {

    protected GitRepo gitRepo

    protected static String testVersion(String event) {
        if (event.startsWith("tag/")) {
            def test = "0.1.0"
            if (event != "tag/release") {
                // Update prerelease tag to be greater than release
                test = "0.1.1-" + event.substring("tag/".length()) + "1"
            }
            return test
        }
        return "build-0123456789ab"
    }

    def checkout(String targetDir = "build-versions") {
        gitRepo = Config.gitAccount.checkoutRepository(Config.global.git.buildVersionsUrl, targetDir, 1)
    }

    def setEventImageVersion(String event, Image image, String version, Map<Object, Object> metadata = null) {
        def dir = "${gitRepo.dir}/image-versions/${event}"
        def path = "${dir}/${Utils.alphaNumericDashLower(image.path)}"
        def txtFile = "${path}.txt"
        def yamlFile = "${path}.yaml"
        // conversion from text to yaml
        if (Config.pipeline.fileExists(txtFile)) {
            Config.pipeline.sh """
                rm "${txtFile}"
            """
        }
        Map<String, Object> data = new LinkedHashMap<>()
        data.put("version", version)
        if(metadata != null && !metadata.isEmpty()) {
            data.put("metadata", metadata)
        }
        def yamlStr = YamlUtils.dump(data)
        Config.pipeline.sh """
            mkdir -p "${dir}"
        """
        Config.pipeline.writeFile(file: yamlFile, text: yamlStr, encoding: "Utf8")
    }

    String getEventImageVersion(String event, Image image) {
        def dir = "${gitRepo.dir}/image-versions/${event}"
        def path = "${dir}/${Utils.alphaNumericDashLower(image.path)}"
        def txtFile = "${path}.txt"
        def yamlFile = "${path}.yaml"
        def result = ""

        if (Config.pipeline.fileExists(txtFile)) {
            result = Config.pipeline.sh(returnStdout: true, script: """
                cat "${txtFile}"
            """)?.trim()
        } else if (Config.pipeline.fileExists(yamlFile)) {
            Map<Object, Object> yamlData = Config.pipeline.readYaml(file: yamlFile) as Map<Object, Object>
            result = yamlData.getOrDefault("version", "")
        }

        return Utils.resultOrTest(result, testVersion(event))
    }

    Map<Object, Object> getEventImageVersionMetadata(String event, Image image) {
        def dir = "${gitRepo.dir}/image-versions/${event}"
        def path = "${dir}/${Utils.alphaNumericDashLower(image.path)}"
        def yamlFile = "${path}.yaml"

        if (Config.pipeline.fileExists(yamlFile)) {
            Map<Object, Object> yamlData = Config.pipeline.readYaml(file: yamlFile) as Map<Object, Object>
            return yamlData.getOrDefault("metadata", new LinkedHashMap<Object, Object>()) as Map<Object, Object>
        }
        return null
    }

    boolean writeEventImageVersion(String event, Image image, String outFile, String format) {
        if (format != "yaml" && format != "env") {
            throw new Exception("invalid format: '${format}'")
        }
        def version = getEventImageVersion(event, image)

        def tag_key = "image_tag_${Utils.alphaNumericUnderscoreLower(image.path)}"
        def event_key = "image_event_${Utils.alphaNumericUnderscoreLower(image.path)}"
        def divider = format == "yaml" ? ": " : "="
        def rc = Config.pipeline.sh(returnStatus: true, script: """
            if [ -n "${version}" ]; then
                echo "${tag_key}${divider}\\"${version}\\"" >> "$outFile"
                echo "${event_key}${divider}\\"${event}\\"" >> "$outFile"
                exit 0
            fi
            exit 1
        """)
        return Utils.resultOrTest(rc == 0, true)
    }

    def writeImageVersion(String version, Image image, String outFile, String format) {
        if (format != "yaml" && format != "env") {
            throw new Exception("invalid format: '${format}'")
        }

        def tag_key = "image_tag_${Utils.alphaNumericUnderscoreLower(image.path)}"
        def event_key = "image_event_${Utils.alphaNumericUnderscoreLower(image.path)}"
        def divider = format == "yaml" ? ": " : "="
        Config.pipeline.sh """
            echo "${tag_key}${divider}\\"${version}\\"" >> "$outFile"
            echo "${event_key}${divider}\\"${version}\\"" >> "$outFile"
        """
    }

    def setRepoEventVersion(String gitRemotePath, String gitTagPrefix, String event, SemVer semVer, Map<Object, Object> metadata = null) {
        def eventWithPrefix = gitTagPrefix ? "${gitTagPrefix}${event}" : event
        def dir = "${gitRepo.dir}/repo-versions/${gitRemotePath}"
        def path = "${dir}/${Utils.alphaNumericDashLower(eventWithPrefix)}"
        def yamlFile = "${path}.yaml"
        def txtFile = "${path}.txt"

        // remove text file for conversion from text to yaml
        if (Config.pipeline.fileExists(txtFile)) {
            Config.pipeline.sh """
                rm "${txtFile}"
            """
        }
        Map<String, Object> data = new LinkedHashMap<>()
        data.put("version", semVer.toString())
        data.put("previousVersion", semVer.getPreviousVersion())
        if(metadata != null && !metadata.isEmpty()) {
            data.put("metadata", metadata)
        }
        def yamlStr = YamlUtils.dump(data)
        Config.pipeline.writeFile(file: yamlFile, text: yamlStr, encoding: "Utf8")
    }

    SemVer getRepoEventVersion(String gitRemotePath, String gitTagPrefix, String event) {
        def eventWithPrefix = gitTagPrefix ? "${gitTagPrefix}${event}" : event
        def dir = "${gitRepo.dir}/repo-versions/${gitRemotePath}"
        def path = "${dir}/${Utils.alphaNumericDashLower(eventWithPrefix)}"
        def yamlFile = "${path}.yaml"
        def txtFile = "${path}.txt"
        String previousVersion = ""

        String version = ""
        if (Config.pipeline.fileExists(txtFile)) {
            version = Config.pipeline.sh(returnStdout: true, script: """
                cat "${txtFile}"
            """)?.trim()
        } else if (Config.pipeline.fileExists(yamlFile)) {
            Map<Object, Object> yamlData = Config.pipeline.readYaml(file: yamlFile) as Map<Object, Object>
            version = yamlData.getOrDefault("version", "")
            previousVersion = yamlData.getOrDefault("previousVersion", "")
        }
        version = Utils.resultOrTest(version, testVersion(event))
        if (version) {
            return new SemVer(version, previousVersion)
        }
        return null
    }

    List<Trigger> getAllJobTriggers() {
        def dir = "${gitRepo.dir}/job-triggers/"
        def result = Config.pipeline.sh(returnStdout: true, script: """
            if [ -d "${dir}" ]; then
                find "${dir}" -type f -name "triggers.yaml" -print0 | xargs -0 cat
            fi
        """)?.trim()
        def testResult = """
- event: commit/master
  eventRegex: null
  imagePaths: [test/b, test/a]
  job: test/master
  params:
  - {\$class: StringParameterValue, name: deploymentKey, value: dev}
- event: tag/rc
  eventRegex: null
  imagePaths: [test/b, test/a]
  job: test/master
  params:
  - {\$class: StringParameterValue, name: deploymentKey, value: stage}
- event: tag/release
  eventRegex: null
  imagePaths: [test/b, test/a]
  job: test/master
  params:
  - {\$class: StringParameterValue, name: deploymentKey, value: prod}
""".trim()
        result = Utils.resultOrTest(result, testResult)
        if (!result) {
            return []
        }
        def triggerInstance = new Trigger()
        List<Trigger> triggers = []
        def triggerList = YamlUtils.load(result)
        triggerList.each { trigger ->
            triggers.add(triggerInstance.newFromObject(trigger))
        }
        return triggers
    }

    def setJobTriggers(String job, List<Trigger> triggers) {
        def dir = "${gitRepo.dir}/job-triggers/${job}"
        def path = "${dir}/triggers.yaml"
        Config.pipeline.sh """
            mkdir -p "${dir}"
        """
        def yamlStr = YamlUtils.hideClassesAndDump([Trigger.class], triggers)
        Config.pipeline.writeFile(file: path, text: yamlStr, encoding: "Utf8")
    }

    def removeJobTriggers(String job) {
        def dir = "${gitRepo.dir}/job-triggers/${job}"
        Config.pipeline.sh """
            rm -rf "${dir}"
        """
    }

    def setImageRepoPath(Image image, String gitRemotePath) {
        def dir = "${gitRepo.dir}/image-repos"
        def path = "${dir}/${Utils.alphaNumericDashLower(image.path)}.txt"
        Config.pipeline.sh """
            mkdir -p "${dir}"
            echo "${gitRemotePath}" > "${path}"
        """
    }

    String getImageRepoPath(Image image) {
        def dir = "${gitRepo.dir}/image-repos"
        def path = "${dir}/${Utils.alphaNumericDashLower(image.path)}.txt"
        def result = Config.pipeline.sh(returnStdout: true, script: """
            if [ -f "${path}" ]; then
                cat "${path}"
            fi
        """)?.trim()
        return Utils.resultOrTest(result, Config.getGitRemotePath(Config.global.git.buildVersionsUrl)) ?: null
    }

    def save() {
        if (gitRepo.hasChanges()) {
            def closure = {
                gitRepo.pull()
                gitRepo.commitAndPush("update build versions")
            }
            if (Config.getGitSelected().buildVersionsLockableResource) {
                Config.pipeline.lock(Config.getGitSelected().buildVersionsLockableResource, closure)
            } else {
                closure()
            }
        }
    }
}
