package com.boxboat.jenkins.pipeline.promote

import com.boxboat.jenkins.library.SemVer
import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.config.PromoteConfig
import com.boxboat.jenkins.library.docker.Image
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

    String getTagType(){
        return promotion.promoteToEvent.substring("tag/".length())
    }

    String getGitCommitToTag(String tag){
        return Utils.buildTagCommit(tag)
    }

    String getGitTagToTag(String tag) {
        def semVer = new SemVer(tag)
        return (semVer.isValid) ? tag : null
    }

    /**
     * Determine the last semver promoted to
    **/
    SemVer getCurrentSemanticVersion(){
        def buildVersions = Config.getBuildVersions()
        return buildVersions.getRepoEventVersion(gitRepo.getRemotePath(), config.gitTagPrefix, promotion.promoteToEvent)
    }

    /**
     * Create a new SemVer from the one provided, bumping its patch or pre-release version
    **/
    SemVer getNextSemanticVersion(SemVer currSemVer) {
        def tagType = getTagType()
        def buildVersions = Config.getBuildVersions()
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
        return nextSemVer
    }

    /**
     * Check if currSemVer was written to this git commit
    **/
    Boolean hasVersionChanged(SemVer currSemVer, String gitCommitToTag, String gitTagToTag) {
        def hasChanged = true
        if (currSemVer != null && !config.gitTagDisable && (gitCommitToTag || gitTagToTag)) {
            def fullGitTagToTag = gitTagToTag
            if (config.gitTagPrefix && gitTagToTag) {
                fullGitTagToTag = "${config.gitTagPrefix}${gitTagToTag}"
            }
            def fullSemVer = currSemVer.toString()
            if (config.gitTagPrefix) {
                fullSemVer = "${config.gitTagPrefix}${fullSemVer}"
            }

            def semVerReferenceHash = gitRepo.getTagReferenceHash(fullSemVer)?.trim()
            if (semVerReferenceHash && (
                    semVerReferenceHash == gitCommitToTag ||
                    semVerReferenceHash == gitRepo.getTagReferenceHash(fullGitTagToTag))) {
                hasChanged = false
                Config.pipeline.echo "No version changes detected"
            }
        }

        return hasChanged
    }

    /**
     * Update each image's tag
    **/
    def updateImageTags(){
        String imageTag
        def buildVersions = Config.getBuildVersions()
        config.images.each { image ->
            image.host = promoteFromRegistry.host
            image.namespace = promoteFromRegistry.namespace

            image.tag = Utils.imageTagFromEvent(promotion.event)
            if (!image.tag) {
                def tag = buildVersions.getEventImageVersion(promotion.event, image)
                if (!tag) {
                    Config.pipeline.error "build-versions does not contain a version for image '${image.path}', event: ${promotion.event}"
                }
                image.tag = tag
            }
            imageTag = image.tag
        }
        return imageTag
    }

    void retagImage(Image image, List<String> tags) {
        def buildVersions = Config.getBuildVersions()
        promoteFromRegistry.withCredentials {
            image.pull()
        }

        imageSummary += "\n${image.path} promoted"
        imageSummary += "\n\tfrom ${formatImageSummary(image, promotion.event, promoteFromRegistry)}"

        pushRegistries.each { pushRegistry ->
            def refImage = image.copy()
            refImage.host = pushRegistry.host
            refImage.namespace = pushRegistry.namespace

            def pushList = []
            tags.eachWithIndex { tag, i ->
                def pushImage = refImage.copy()
                pushImage.tag = tag
                pushList.add(pushImage)
                image.reTag(tag)

                def start = (i == 0) ? "\n\tto" : "\n\t  "
                imageSummary = "${start}   ${formatImageSummary(pushImage, promotion.promoteToEvent, pushRegistry)}"
            }

            pushRegistry.withCredentials {
                pushList.each { pushImage ->
                    pushImage.push()
                }
            }
        }
        buildVersions.setEventImageVersion(pushEvent, image, nextSemVer.toString())
    }

    /**
     * Promote all images to the nextSemVer
    **/
    def promoteImages(def nextSemVer){
        def buildVersions = Config.getBuildVersions()
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
            def tags = [nextSemVer.toString, refTag]
            config.images.each { image ->
                retagImage(image, tags)
            }
            buildVersions.setRepoEventVersion(gitRepo.getRemotePath(), config.gitTagPrefix, pushEvent, nextSemVer)
        }

        if (getTagType() == "release") {
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
    }

    /**
     * Tag the project's git repo with the Docker image tag if appropriate
    **/
    def tagGitRepo(def gitTag, def gitCommitToTag, def gitTagToTag) {
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
            if (config.gitTagPrefix) {
                gitTag = "${config.gitTagPrefix}${gitTag}"
            }
            gitRepo.tagAndPush(gitTag)
        }
    }

    /**
     * Promote images to a new tag if the git hash has changed, and write the new tag back to git
    **/
    def promote() {
        String gitTagToTag
        String imageTag = updateImageTags()
        String gitCommitToTag = Utils.buildTagCommit(imageTag)
        if (!gitCommitToTag) {
            def semVer = new SemVer(imageTag)
            if (semVer.isValid) {
                gitTagToTag = imageTag
            }
        }

        def currSemVer = getCurrentSemanticVersion()
        def nextSemVer = getNextSemanticVersion(currSemVer)
        versionChange = hasVersionChanged(currSemVer, gitCommitToTag, gitTagToTag)

        if (versionChange) {
            promoteImages(nextSemVer)
            tagGitRepo(nextSemVer.toString(), gitCommitToTag, gitTagToTag)
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
