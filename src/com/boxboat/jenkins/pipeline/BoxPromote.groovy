package com.boxboat.jenkins.pipeline


import com.boxboat.jenkins.library.SemVer
import com.boxboat.jenkins.library.ServerConfig
import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.docker.Image
import com.boxboat.jenkins.library.docker.Registry

class BoxPromote extends BoxBase {

    public List<String> images
    public String existingTag
    public String newTag
    private SemVer _newSemVer

    BoxPromote(Map config) {
        super(config)
        config?.each { k, v -> this[k] = v }
    }

    static def createBoxPromote(Map config) {
        def promote = new BoxDeploy(config)
        promote.steps.properties([
            [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', numToKeepStr: '100']]
        ])
        return promote
    }

    def init() {
        super.init()
        if (!images || images.size() == 0) {
            steps.error "'images' must be set"
        }
        if (!existingTag) {
            steps.error "'existingTag' must be set"
        }
        if (!newTag) {
            steps.error "'newTag' must be set"
        }
        _newSemVer = new SemVer(newTag)
        if (!_newSemVer.isValid) {
            steps.error "Tag '${newTag}' is not a valid Semantic Version"
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

        def buildVersions = gitAccount.checkoutRepository(ServerConfig.buildVersionsGitRemoteUrl, "build-versions", 1)
        def updateBuildVersions = false

        Registry registry = ServerConfig.registryMap.get("dtr")
        steps.docker.withRegistry(
            "${registry.scheme}://${registry.uri}",
                registry.credentials) {

            List<Image> images = images.collect { String v -> Image.fromImageString(v) }

            images.each { Image image ->
                def pullImage = image.copy()
                pullImage.host = registry.uri
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
                pushImage.host = registry.uri
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
