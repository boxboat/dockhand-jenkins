package com.boxboat.jenkins.pipeline.promote

import com.boxboat.jenkins.library.SemVer
import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.config.PromoteConfig
import com.boxboat.jenkins.library.docker.Registry
import com.boxboat.jenkins.library.promote.Promotion
import com.boxboat.jenkins.pipeline.BoxBase

class BoxPromote extends BoxBase<PromoteConfig> implements Serializable {

    public String overrideEvent
    public String registryKey

    protected String imageSummary
    protected SemVer baseSemVer
    protected Promotion promotion
    protected Registry promoteFromRegistry

    protected boolean versionChange = true

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

        def buildVersions = Config.getBuildVersions()

        String gitCommitToTag
        String gitTagToTag
        config.images.each { image ->
            image.host = promoteFromRegistry.host
            image.namespace = promoteFromRegistry.namespace
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

        def currSemVer = buildVersions.getRepoEventVersion(gitRepo.getRemotePath(), config.gitTagPrefix, promotion.promoteToEvent)
        def nextSemVer = currSemVer?.copy()
        if (nextSemVer == null || !nextSemVer.isValid) {
            nextSemVer = baseSemVer.copy()
        } else if (tagType == "release") {
            nextSemVer.patch++
        }
        if (tagType != "release") {
            def releaseSemVer = buildVersions.getRepoEventVersion(gitRepo.getRemotePath(), config.gitTagPrefix, "tag/release")
            if (releaseSemVer != null && releaseSemVer.isValid && releaseSemVer >= nextSemVer) {
                nextSemVer = releaseSemVer.copy()
                nextSemVer.patch++
            }
            nextSemVer.incrementPreRelease(tagType)
        }

        if (currSemVer != null && !config.gitTagDisable && (gitCommitToTag || gitTagToTag)) {
            def fullGitTagToTag = gitTagToTag
            if (config.gitTagPrefix && gitTagToTag) {
                fullGitTagToTag = "${config.gitTagPrefix}${gitTagToTag}"
            }
            def fullSemVer = currSemVer.toString()
            if (config.gitTagPrefix) {
                fullSemVer = "${config.gitTagPrefix}${fullSemVer}"
            }

            if (gitRepo.getTagReferenceHash(fullSemVer) == gitCommitToTag ||
                    gitRepo.getTagReferenceHash(fullSemVer) == gitRepo.getTagReferenceHash(fullGitTagToTag)) {
                versionChange = false
                Config.pipeline.echo "No version changes detected"
            }
        }

        if (versionChange) {
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

                def refTag = "tag-${Utils.alphaNumericDashLower(pushEvent.substring("tag/".length()))}"
                if (refTag == "tag-release") {
                    refTag = "latest"
                }

                config.images.each { image ->
                    promoteFromRegistry.withCredentials() {
                        image.pull()
                    }

                    imageSummary += "\n${image.path} promoted"
                    imageSummary += "\n\tfrom ${formatImageSummary(image, promotion.event, promoteFromRegistry)}"

                    pushRegistries.each { pushRegistry ->
                        def newImageSemVer = image.copy()
                        newImageSemVer.host = pushRegistry.host
                        newImageSemVer.namespace = pushRegistry.namespace
                        newImageSemVer.tag = nextSemVer.toString()
                        def newImageRef = image.copy()
                        newImageRef.tag = refTag
                        image.reTag(newImageSemVer)
                        image.reTag(newImageRef)
                        pushRegistry.withCredentials() {
                            newImageSemVer.push()
                            newImageRef.push()
                        }

                        imageSummary += "\n\tto   ${formatImageSummary(newImageSemVer, promotion.promoteToEvent, pushRegistry)}"
                        imageSummary += "\n\t     ${formatImageSummary(newImageRef, promotion.promoteToEvent, pushRegistry)}"
                    }
                    buildVersions.setEventImageVersion(pushEvent, image, nextSemVer.toString())
                }
                buildVersions.setRepoEventVersion(gitRepo.getRemotePath(), config.gitTagPrefix, pushEvent, nextSemVer)
            }

            if (tagType == "release") {
                config.promotionMap.keySet().toList().each { k ->
                    def v = config.promotionMap[k]
                    if (v.promoteToEvent?.startsWith("tag/")) {
                        def currentSemVer = buildVersions.getRepoEventVersion(gitRepo.getRemotePath(), config.gitTagPrefix, v.promoteToEvent)
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

            if (!config.gitTagDisable && (gitCommitToTag || gitTagToTag)) {
                // resetting will remove build-versions
                Config.resetBuildVersions()
                if (gitCommitToTag) {
                    gitRepo.fetchAndCheckoutCommit(gitCommitToTag)
                } else if (gitTagToTag) {
                    if (config.gitTagPrefix) {
                        gitTagToTag = "${config.gitTagPrefix}${gitTagToTag}"
                    }
                    gitRepo.fetchAndCheckoutTag(gitTagToTag)
                }
                def gitTag = nextSemVer.toString()
                if (config.gitTagPrefix) {
                    gitTag = "${config.gitTagPrefix}${gitTag}"
                }
                gitRepo.tagAndPush(gitTag)
            }
        }
    }

    def summary() {
        if (versionChange) {
            pipelineSummaryMessage = """
Promoted '${config.promotionKey}' from '${promotion.event}' to '${promotion.promoteToEvent}'
Commit: ${gitRepo.commitUrl}

${imageSummary}

${buildUser}
        """
        } else {
            pipelineSummaryMessage = "No image promotion occurred because no version changes were detected"
        }
        super.summary()
    }
}
