package com.boxboat.jenkins.pipeline.promote

import com.boxboat.jenkins.library.SemVer
import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.config.PromoteConfig
import com.boxboat.jenkins.library.docker.Image
import com.boxboat.jenkins.library.docker.Registry
import com.boxboat.jenkins.pipeline.BoxBase

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
            Config.pipeline.error "'images' must be set"
        }
        if (!checkout && !event) {
            Config.pipeline.error "'checkout' or 'event' must be set"
        }
        if (!promoteToEvent) {
            Config.pipeline.error "'promoteToEvent' must be set"
        }
        if (!baseVersion) {
            Config.pipeline.error "'baseVersion' must be set"
        }
    }

    def promote() {
        def events = ["tag-pre"]
        if (!_newSemVer.isPreRelease) {
            events.add("tag")
        }
        Config.pipeline.echo "Promoting images '${images.join("', '")}' from '${existingTag}' to '${newTag}' " +
                "for event(s) '${events.join("', '")}'"

        def script = ""
        events.each { event ->
            script += """
                mkdir -p "build-versions/${event}"
            """
        }

        def buildVersions = gitAccount.checkoutRepository(Config.global.git.buildVersionsUrl, "build-versions", 1)
        def updateBuildVersions = false

        Registry registry = Config.global.getRegistry(registryConfig)
        Config.pipeline.docker.withRegistry(
                registry.getRegistryUrl(),
                registry.credential) {

            List<Image> images = images.collect { String v -> new Image(v) }

            images.each { Image image ->
                def pullImage = image.copy()
                pullImage.host = registry.host
                pullImage.tag = existingTag
                pullImage.pull()
            }

            events.each { event ->
                images.each { Image image ->
                    def filePath = "build-versions/${event}/${Utils.alphaNumericDashLower(image.path)}.yaml"
                    def currentTag = Config.pipeline.sh(
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
                        Config.pipeline.echo "This is the first version for image '${image.path}', event '${event}'; " +
                                "adding build version"
                        updateBuildVersion()
                    } else if (_newSemVer.compareTo(currentSemVer) > 0) {
                        Config.pipeline.echo "Image '${image.path}' version '${newTag}' is newer than existing version " +
                                "'${currentVersion}' for event '${event}'; " +
                                "updating build version"
                        updateBuildVersion()
                    } else {
                        Config.pipeline.echo "Image '${image.path}' version '${newTag}' is " +
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
                image.reTag(pushImage)
                pushImage.push()
            }

            if (updateBuildVersions) {
                Config.pipeline.sh script
                buildVersions.commitAndPush("update build-versions")
            }

        }
    }

}
