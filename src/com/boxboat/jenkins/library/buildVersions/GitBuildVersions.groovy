package com.boxboat.jenkins.library.buildVersions

import com.boxboat.jenkins.library.SemVer
import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.docker.Image
import com.boxboat.jenkins.library.git.GitAccount
import com.boxboat.jenkins.library.git.GitRepo
import com.boxboat.jenkins.library.trigger.Trigger
import com.boxboat.jenkins.library.yaml.YamlUtils

class GitBuildVersions implements Serializable {

    protected GitRepo gitRepo

    protected static String testVersion(String event) {
        if (event.startsWith("tag/")) {
            def test = "0.1.0"
            if (event != "tag/release") {
                test += "-" + event.substring("tag/".length()) + "1"
            }
            return test
        }
        return "build-0123456789ab"
    }

    def checkout(GitAccount gitAccount, String targetDir = "build-versions") {
        gitRepo = gitAccount.checkoutRepository(Config.global.git.buildVersionsUrl, targetDir, 1)
    }

    def setEventImageVersion(String event, Image image, String version) {
        def dir = "${gitRepo.dir}/image-versions/${event}"
        def path = "${dir}/${Utils.alphaNumericDashLower(image.path)}.txt"
        Config.pipeline.sh """
            mkdir -p "${dir}"
            echo "${version}" > "${path}"
        """
    }

    String getEventImageVersion(String event, Image image) {
        def dir = "${gitRepo.dir}/image-versions/${event}"
        def path = "${dir}/${Utils.alphaNumericDashLower(image.path)}.txt"
        def result = Config.pipeline.sh(returnStdout: true, script: """
            if [ -f "${path}" ]; then
                cat "${path}"
            fi
        """)?.trim()
        return Utils.resultOrTest(result, testVersion(event))
    }

    boolean writeEventImageVersion(String event, Image image, String outFile, String format) {
        if (format != "yaml" && format != "env") {
            throw new Exception("invalid format: '${format}'")
        }

        def dir = "${gitRepo.dir}/image-versions/${event}"
        def path = "${dir}/${Utils.alphaNumericDashLower(image.path)}.txt"
        def key = "image_tag_${Utils.alphaNumericUnderscoreLower(image.path)}"
        def divider = format == "yaml" ? ": " : "="
        def rc = Config.pipeline.sh(returnStatus: true, script: """
            if [ -f "${path}" ]; then
                version=\$(cat "$path")
                echo "${key}${divider}\\"\$version\\"" >> "$outFile"
                exit 0
            fi
            exit 1
        """)
        return Utils.resultOrTest(rc == 0, true)
    }

    def setRepoEventVersion(String gitRemotePath, String event, SemVer semVer) {
        def dir = "${gitRepo.dir}/repo-versions/${gitRemotePath}"
        def path = "${dir}/${Utils.alphaNumericDashLower(event)}.txt"
        Config.pipeline.sh """
            mkdir -p "${dir}"
            echo '${semVer.toString()}' > "${path}"
        """
    }

    SemVer getRepoEventVersion(String gitRemotePath, String event) {
        def dir = "${gitRepo.dir}/repo-versions/${gitRemotePath}"
        def path = "${dir}/${Utils.alphaNumericDashLower(event)}.txt"
        String version = Config.pipeline.sh(returnStdout: true, script: """
            if [ -f "${path}" ]; then
                cat "${path}"
            fi
        """)?.trim()
        version = Utils.resultOrTest(version, testVersion(event))
        if (version) {
            return new SemVer(version)
        }
        return null
    }

    List<Trigger> getAllJobTriggers() {
        def dir = "${gitRepo.dir}/job-triggers/"
        def result = Config.pipeline.sh(returnStdout: true, script: """
            if [ -d "${dir}" ]; then
                find "${dir}" -type f -name "triggers.yaml" | xargs cat
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

    def save() {
        if (gitRepo.hasChanges()) {
            def closure = {
                gitRepo.pull()
                gitRepo.commitAndPush("update build versions")
            }
            if (Config.global.git.buildVersionsLockableResource) {
                Config.pipeline.lock(Config.global.git.buildVersionsLockableResource, closure)
            } else {
                closure()
            }
        }
    }
}
