package com.boxboat.jenkins.pipeline.promote

import com.boxboat.jenkins.library.SemVer
import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.config.PromoteConfig
import com.boxboat.jenkins.library.registry.Registry
import com.boxboat.jenkins.library.promote.Promotion
import com.boxboat.jenkins.pipeline.BoxBase

class BoxPromote extends BoxBase<PromoteConfig> implements Serializable {

    public String overrideEvent
    public String registryKey

    protected String imageSummary
    protected SemVer baseSemVer
    protected Promotion promotion
    protected Registry promoteFromRegistry

    BoxPromote(Map config = [:]) {
        super(config)
        setPropertiesFromMap(config)
    }

    @Override
    protected String configKey() {
        return "promote"
    }

    @Override
    def init() {
        super.init()
        if (!config.promotionKey) {
            // abort, since pipeline may refresh without any parameters
            Config.pipeline.currentBuild.result = 'ABORTED'
            Config.pipeline.error "'config.promotionKey' must be set"
        }
        promotion = config.getPromotion(config.promotionKey)
        if (overrideEvent) {
            promotion.event = overrideEvent
        }
        String messageBase = "Promotion '${config.promotionKey}' from '${promotion.event}' to '${promotion.promoteToEvent}'"
        notifySuccessMessage = "${messageBase} succeeded"
        notifyFailureMessage = "${messageBase} failed"

        buildDescription = "${config.promotionKey} - ${promotion.event} - ${promotion.promoteToEvent} - ${buildUser}"

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
        if (!promotion.promoteToEvent.startsWith("tag/")) {
            Config.pipeline.error "'promoteToEvent' must start with 'tag/'"
        }
        emitEvents.add(promotion.promoteToEvent)
        if (registryKey) {
            promoteFromRegistry = Config.global.getRegistry(registryKey)
        } else if (Utils.isImageTagEvent(promotion.event)) {
            if (Config.global.registryMap.size() == 1) {
                promoteFromRegistry = Config.global.getRegistry(Config.global.registryMap.keySet().toList().first())
            }
        } else {
            def registries = config.getEventRegistries(promotion.event)
            if (registries.size() > 0) {
                promoteFromRegistry = registries[0]
            }
        }
        if (!promoteFromRegistry) {
            Config.pipeline.error "'registryKey' must be set"
        }
    }

    def promote() {
        def tagType = promotion.promoteToEvent.substring("tag/".length())

        def buildVersions = this.getBuildVersions()

        String gitCommitToTag
        String gitTagToTag
        config.images.each { image ->
            image.host = promoteFromRegistry.host
            if (Utils.isImageTagEvent(promotion.event)) {
                image.tag = Utils.imageTagFromEvent(promotion.event)
            } else {
                def tag = buildVersions.getEventImageVersion(promotion.event, image)
                if (!tag) {
                    Config.pipeline.error "build-versions does not contain a version for image '${image.path}', event: ${promotion.event}"
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
            if (releaseSemVer != null && releaseSemVer.isValid && releaseSemVer > nextSemVer) {
                nextSemVer = releaseSemVer.copy()
                nextSemVer.patch++
            }
            nextSemVer.incrementPreRelease(tagType)
        }
        imageSummary = "Images"
        config.images.each { image ->
            Config.pipeline.echo "Promoting '${image.path}' from '${image.tag}' to '${nextSemVer.toString()}'"
            notifySuccessMessage += "\n${image.path} promoted from '${image.tag}' to '${nextSemVer.toString()}'"
        }
        if (!trigger) {
            Config.pipeline.timeout(time: 10, unit: 'MINUTES') {
                Config.pipeline.input "Upgrade images to version '${nextSemVer.toString()}'?"
            }
        }

        def promoteClosure = { String pushEvent ->
            def pushRegistries = config.getEventRegistries(pushEvent)
            if (pushRegistries.size() == 0) {
                Config.pipeline.error "'config.eventRegistryKeys' must specify a registry for event '${pushEvent}'"
            }

            config.images.each { image ->
                promoteFromRegistry.withCredentials() {
                    image.pull()
                }

                imageSummary += "\n${image.path} promoted"
                imageSummary += "\n\tfrom ${formatImageSummary(promoteFromRegistry, image)}"

                pushRegistries.each { pushRegistry ->
                    def newImage = image.copy()
                    newImage.host = pushRegistry.host
                    newImage.tag = nextSemVer.toString()
                    image.reTag(newImage)
                    pushRegistry.withCredentials() {
                        newImage.push()
                    }

                    imageSummary += "\n\tto   ${formatImageSummary(pushRegistry, newImage)}"
                }
                buildVersions.setEventImageVersion(pushEvent, image, nextSemVer.toString())
            }
            buildVersions.setRepoEventVersion(gitRepo.getRemotePath(), pushEvent, nextSemVer)
        }

        if (tagType == "release") {
            config.promotionMap.keySet().toList().each { k ->
                def v = config.promotionMap[k]
                if (v.promoteToEvent?.startsWith("tag/")) {
                    def currentSemVer = buildVersions.getRepoEventVersion(gitRepo.getRemotePath(), v.promoteToEvent)
                    if (nextSemVer > currentSemVer) {
                        emitEvents.add(v.promoteToEvent)
                        promoteClosure(v.promoteToEvent)
                    }
                }
            }
        } else {
            promoteClosure(promotion.promoteToEvent)
        }

        buildVersions.save()

        if (gitCommitToTag || gitTagToTag) {
            // resetting will remove build-versions
            _buildVersions = null
            if (gitCommitToTag) {
                gitRepo.fetchAndCheckoutCommit(gitCommitToTag)
            } else if (gitTagToTag) {
                gitRepo.fetchAndCheckoutTag(gitTagToTag)
            }
            gitRepo.tagAndPush(nextSemVer.toString())
        }

    }

    def summary() {
        pipelineSummaryMessage = """
Promoted '${config.promotionKey}' from '${promotion.event}' to '${promotion.promoteToEvent}'
Commit: ${gitRepo.commitUrl}

${imageSummary}

${buildUser}
        """
        super.summary()
    }
}
