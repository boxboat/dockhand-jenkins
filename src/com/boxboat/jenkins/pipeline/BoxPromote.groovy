package com.boxboat.jenkins.pipeline

import com.boxboat.jenkins.library.SemVer
import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.GlobalConfig
import com.boxboat.jenkins.library.config.PromoteConfig
import com.boxboat.jenkins.library.docker.Image
import com.boxboat.jenkins.library.docker.Registry

class BoxPromote extends BoxBase<PromoteConfig> {

    BoxPromote(Map config) {
        super(config)
    }

    @Override
    protected String configKey() {
        return "promote"
    }

    static def create(Map config) {
        def promote = new BoxPromote(config)
        return promote
    }

    def init() {
        super.init()
        if (!images || images.size() == 0) {
            steps.error "'images' must be set"
        }
        if (!checkout && !event) {
            steps.error "'checkout' or 'event' must be set"
        }
        if (!promoteToEvent) {
            steps.error "'promoteToEvent' must be set"
        }
        if (!baseVersion) {
            steps.error "'baseVersion' must be set"
        }
    }

    def promote() {
        def events = ["tag-pre"]
        if (!_newSemVer.isPreRelease) {
            events.add("tag")
        }
        steps.echo "Promoting images '${images.join("', '")}' from '${existingTag}' to '${newTag}' " +
                "for event(s) '${events.join("', '")}'"

        def script = ""
        events.each { event ->
            script += """
                mkdir -p "build-versions/${event}"
            """
        }

        def buildVersions = gitAccount.checkoutRepository(GlobalConfig.config.git.buildVersionsUrl, "build-versions", 1)
        def updateBuildVersions = false

        Registry registry = GlobalConfig.config.getRegistry(registryConfig)
        steps.docker.withRegistry(
                registry.getRegistryUrl(),
                registry.credential) {

            List<Image> images = images.collect { String v -> new Image(v) }

            images.each { Image image ->
                def pullImage = image.copy()
                pullImage.host = registry.host
                pullImage.tag = existingTag
                pullImage.pull(steps)
            }

            events.each { event ->
                images.each { Image image ->
                    def filePath = "build-versions/${event}/${Utils.alphaNumericDashLower(image.path)}.yaml"
                    def currentTag = steps.sh(
                            returnStdout: true,
                            script: """
                                if [ -f "${filePath}" ]; then
                                    cat "${filePath}" | head -n 1
                                fi
                            """)
                    String currentVersion = null
                    SemVer currentSemVer = null
                    if (currentTag) {
                        def matcher = currentTag =~ /: "(.*)"$/
                        currentVersion = matcher.hasGroup() ? matcher[0][1] : null
                        if (currentVersion) {
                            currentSemVer = new SemVer(currentVersion)
                        }
                    }
                    def updateBuildVersion = {
                        script += """
                        echo 'image_tag_${Utils.alphaNumericUnderscoreLower(image.path)}: "${newTag}"' \\
                            > "${filePath}"
                        """
                        updateBuildVersions = true
                    }
                    if (!currentSemVer) {
                        steps.echo "This is the first version for image '${image.path}', event '${event}'; " +
                                "adding build version"
                        updateBuildVersion()
                    } else if (_newSemVer.compareTo(currentSemVer) > 0) {
                        steps.echo "Image '${image.path}' version '${newTag}' is newer than existing version " +
                                "'${currentVersion}' for event '${event}'; " +
                                "updating build version"
                        updateBuildVersion()
                    } else {
                        steps.echo "Image '${image.path}' version '${newTag}' is " +
                                (_newSemVer.compareTo(currentSemVer) == 0 ? "the same as" : "older than") +
                                " existing version " +
                                "'${currentVersion}' for event '${event}'; " +
                                "not updating build version"
                    }
                }
            }

            images.each { Image image ->
                def pushImage = image.copy()
                pushImage.host = registry.host
                pushImage.tag = newTag
                image.reTag(steps, pushImage)
                pushImage.push(steps)
            }

            if (updateBuildVersions) {
                steps.sh script
                buildVersions.commitAndPush("update build-versions")
            }

        }
    }

}
