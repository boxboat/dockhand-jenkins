package com.boxboat.jenkins.pipeline.build

import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.buildVersions.GitBuildVersions
import com.boxboat.jenkins.library.config.BuildConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.docker.Compose
import com.boxboat.jenkins.library.docker.Image
import com.boxboat.jenkins.pipeline.BoxBase

class BoxBuild extends BoxBase<BuildConfig> implements Serializable {

    BoxBuild(Map config = [:]) {
        super(config)
    }

    @Override
    protected String configKey() {
        return "build"
    }

    def init() {
        super.init()
        // ensure tag is set
        config.images.each { image ->
            if (!image.tag) {
                image.tag = gitRepo.shortHash
            }
        }
        // pull images
        config.pullImages.each { image ->
            Config.pipeline.sh """
                docker pull "${image}"
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
        def branch = gitRepo.branch?.toLowerCase()
        def event = Utils.cleanEvent("commit/${branch}")
        def eventTag = Utils.cleanTag(event)
        Config.pipeline.echo branch
        def buildTag = "build-${gitRepo.shortHash}"
        def registries = config.getEventRegistries(event)

        if (registries) {
            def isBranchTip = gitRepo.isBranchTip()
            def tags = [buildTag]
            if (isBranchTip) {
                tags.add(eventTag)
            }

            registries.each { registry ->
                Config.pipeline.docker.withRegistry(
                        registry.getRegistryUrl(),
                        registry.credential) {
                    config.images.each { Image image ->
                        tags.each { String tag ->
                            def newImage = image.copy()
                            newImage.host = registry.host
                            newImage.tag = tag
                            image.reTag(newImage)
                            newImage.push()
                        }
                    }
                }
            }

            if (isBranchTip) {
                def buildVersions = new GitBuildVersions()
                buildVersions.checkout(gitAccount)
                config.images.each { image ->
                    buildVersions.setEventImageVersion(event, image, buildTag)
                }
                buildVersions.save()
            }
        }

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
