package com.boxboat.jenkins.pipeline.promote

import com.boxboat.jenkins.library.SemVer
import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.buildVersions.GitBuildVersions
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.config.PromoteConfig
import com.boxboat.jenkins.library.docker.Registry
import com.boxboat.jenkins.library.promote.Promotion
import com.boxboat.jenkins.pipeline.BoxBase

class BoxPromote extends BoxBase<PromoteConfig> {

    public String promotionKey
    public String registryKey
    protected SemVer baseSemVer
    protected Promotion promotion
    protected Registry promoteFromRegistry

    BoxPromote(Map config = [:]) {
        super(config)
    }

    @Override
    protected String configKey() {
        return "promote"
    }

    def init() {
        super.init()
        if (!config.images || config.images.size() == 0) {
            Config.pipeline.error "'config.images' must be set"
        }
        if (!config.baseVersion) {
            Config.pipeline.error "'config.baseVersion' must be set"
        }
        baseSemVer = new SemVer(config.baseVersion)
        if (!baseSemVer.isValid) {
            Config.pipeline.error "'config.baseVersion' is not a valid Semantic Version"
        }
        if (!promotionKey) {
            Config.pipeline.error "'promotionKey' must be set"
        }
        promotion = config.getPromotion(promotionKey)
        if (!promotion.promoteToEvent.startsWith("tag/")) {
            Config.pipeline.error "'promoteToEvent' must start with 'tag/'"
        }
        if (!event) {
            event = promotion.event
        }
        if (registryKey) {
            promoteFromRegistry = Config.global.getRegistry(registryKey)
        } else {
            def registries = config.getEventRegistries(event)
            if (registries.size() == 0) {
                Config.pipeline.error "'registryKey' must be set"
            }
            promoteFromRegistry = registries[0]
        }
    }

    def promote() {
        def tagType = promotion.promoteToEvent.substring("tag/".length())

        def buildVersions = new GitBuildVersions()
        buildVersions.checkout(gitAccount)

        String gitCommitToTag
        String gitTagToTag
        config.images.each { image ->
            image.host = promoteFromRegistry.host
            if (event.startsWith("image-tag/")) {
                image.tag = event.substring("image-tag/".length())
            } else {
                def tag = buildVersions.getEventImageVersion(event, image)
                if (!tag) {
                    Config.pipeline.error "build-versions does not contain a version for image '${image.path}', event: ${event}"
                }
                image.tag = tag
            }
            if (!gitCommitToTag && !gitTagToTag) {
                gitCommitToTag = Utils.buildTagCommit(image.tag)
                if (!gitCommitToTag) {
                    def semVer = new SemVer(image.tag)
                    if (semVer.isValid) {
                        gitTagToTag = image.tag
                    }
                }
            }
        }

        def nextSemVer = buildVersions.getRepoEventVersion(gitRepo.getRemotePath(), promotion.promoteToEvent)
        if (nextSemVer == null || !nextSemVer.isValid) {
            nextSemVer = baseSemVer.copy()
        } else if (tagType == "release") {
            nextSemVer.patch++
        }
        if (tagType != "release") {
            def releaseSemVer = buildVersions.getRepoEventVersion(gitRepo.getRemotePath(), "tag/release")
            if (releaseSemVer != null && releaseSemVer > nextSemVer) {
                nextSemVer = releaseSemVer.copy()
                nextSemVer.patch++
            }
            nextSemVer.incrementPreRelease(tagType)
        }

        config.images.each { image ->
            Config.pipeline.echo "Promoting '${image.path}' from '${image.tag}' to '${nextSemVer.toString()}'"
        }
        if (!trigger) {
            Config.pipeline.timeout(time: 10, unit: 'MINUTES') {
                Config.pipeline.input "Upgrade images to versin '${nextSemVer.toString()}'?"
            }
        }

        def promoteClosure = { String pushEvent ->
            def pushRegistries = config.getEventRegistries(pushEvent)
            if (pushRegistries.size() == 0) {
                Config.pipeline.error "'config.eventRegistryKeys' must specify a registry for event '${pushEvent}'"
            }

            config.images.each { image ->
                image.pull()
                pushRegistries.each { pushRegistry ->
                    def newImage = image.copy()
                    newImage.host = pushRegistry.host
                    newImage.tag = nextSemVer.toString()
                    image.reTag(newImage)
                    newImage.push()
                }
                buildVersions.setEventImageVersion(pushEvent, image, nextSemVer.toString())
            }
            buildVersions.setRepoEventVersion(gitRepo.getRemotePath(), pushEvent, nextSemVer)
        }

        if (tagType == "release") {
            def keys = config.promotionMap.keySet().toArray()
            keys.each { k ->
                def v = config.promotionMap.get(k)
                if (v.promoteToEvent?.startsWith("tag/")) {
                    def currentSemVer = buildVersions.getRepoEventVersion(gitRepo.getRemotePath(), v.promoteToEvent)
                    if (nextSemVer > currentSemVer) {
                        promoteClosure(v.promoteToEvent)
                    }
                }
            }
        } else {
            promoteClosure(promotion.promoteToEvent)
        }

        buildVersions.save()


        if (gitCommitToTag) {
            gitRepo.fetchAndCheckoutCommit(gitCommitToTag)
            gitRepo.tagAndPush(nextSemVer.toString())
        } else if (gitTagToTag) {
            gitRepo.fetchAndCheckoutTag(gitTagToTag)
            gitRepo.tagAndPush(nextSemVer.toString())
        }

    }

}
