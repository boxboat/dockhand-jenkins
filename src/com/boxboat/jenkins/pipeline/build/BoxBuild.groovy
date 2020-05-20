package com.boxboat.jenkins.pipeline.build

import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.buildVersions.GitBuildVersions
import com.boxboat.jenkins.library.config.BuildConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.docker.Compose
import com.boxboat.jenkins.library.docker.Image
import com.boxboat.jenkins.pipeline.BoxBase

class BoxBuild extends BoxBase<BuildConfig> implements Serializable {

    protected String imageSummary

    BoxBuild(Map config = [:]) {
        super(config)
        setPropertiesFromMap(config)
    }

    @Override
    protected String configKey() {
        return "build"
    }

    @Override
    def init() {
        super.init()
        String notifyMessage = "Build for branch '${gitRepo.branch}' commit '${gitRepo.shortHash}'"
        notifySuccessMessage = "${notifyMessage} succeeded"
        notifyFailureMessage = "${notifyMessage} failed"
        buildDescription = "${gitRepo.branch} - ${gitRepo.shortHash} - ${buildUser}"

        // ensure tag is set
        config.images.each { image ->
            if (!image.tag) {
                image.tag = gitRepo.shortHash
            }
        }
        // pull images
        config.pullImages.each { image ->
            Config.pipeline.sh """
                docker pull "${image.getUrl()}"
            """
        }
    }

    def composeBuild(String profile) {
        Compose.build(config.composeProfileMap.get(profile), profile)
    }

    def composeUp(String profile) {
        // clean up all profiles
        this.composeCleanup()
        // start the specified profile
        Compose.up(config.composeProfileMap.get(profile), profile)
    }

    def composeDown(String profile) {
        Compose.down(config.composeProfileMap.get(profile), profile)
    }

    def push() {
        def branch = gitRepo.getBranch()
        def event = "commit/${branch}"
        def eventTag = Utils.cleanTag(event)
        Config.pipeline.echo branch
        def buildTag = "build-${gitRepo.shortHash}"
        def registries = config.getEventRegistries(event)

        if (registries) {
            def isBranchTip = gitRepo.isBranchTip()
            def tags = [buildTag.toString()]
            if (isBranchTip) {
                tags.add(eventTag)
            }

            imageSummary = imageSummaryHeader()
            registries.each { registry ->
                registry.withCredentials {
                    config.images.each { Image image ->
                        tags.each { String tag ->
                            def newImage = image.copy()
                            newImage.host = registry.host
                            newImage.namespace = registry.namespace
                            newImage.tag = tag
                            image.reTag(newImage)
                            newImage.push()
                            imageSummary += "\n${formatImageSummary(newImage, event, registry)}"
                        }
                    }
                }
            }

            if (isBranchTip) {
                Config.pipeline.echo "isBranchTip: ${event}"
                emitEvents.add(event)
                def buildVersions = Config.getBuildVersions()
                def repoPath = this.gitRepo.getRemotePath()
                config.images.each { image ->
                    buildVersions.setEventImageVersion(event, image, buildTag)
                    buildVersions.setImageRepoPath(image, repoPath)
                }
                buildVersions.save()
            }
        }

    }

    def summary() {
        String triggeredBuilds = ""


        if (emitBuilds) {
            for (build in emitBuilds) {
                triggeredBuilds += "\n${build}"
            }
            triggeredBuilds += "\n"
        }
        pipelineSummaryMessage = """
Build for branch '${gitRepo.branch}' commit '${gitRepo.shortHash}'
Branch: ${gitRepo.branchUrl}
Commit: ${gitRepo.commitUrl}

${imageSummary}
${triggeredBuilds}
${buildUser}
        """
        super.summary()
    }

    protected composeCleanup() {
        config?.composeProfileMap?.each { profile, dir ->
            composeDown(profile)
        }
    }

    def cleanup() {
        this.composeCleanup()
        super.cleanup()
    }

}
