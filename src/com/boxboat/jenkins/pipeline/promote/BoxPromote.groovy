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
    public String promoteToVersion
    public String registryKey

    protected String imageSummary
    protected SemVer baseSemVer
    protected Promotion promotion
    protected Registry promoteFromRegistry

    protected boolean versionChange = true
    protected boolean writebackBuildVersions = true

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

    /**
     * Parse the promoteToEvent to determine the type of promote (tag/release, or tag/<pre-release>)
    **/
    String getTagType() {
        return promotion.promoteToEvent.substring("tag/".length())
    }

    /**
     * Get the git commit (or git tag) that used for this promote
    **/
    List<String> getGitCommit(){
        String gitCommitToTag
        String gitTagToTag
        def buildVersions = Config.getBuildVersions()
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
        return [gitCommitToTag, gitTagToTag]
    }


    /**
     * Get the current semVer for promotion
    **/
    SemVer getCurrSemVer() {
        // getBuildVersions returns singleton - If buildVersions already checked out, will initialized object
        def buildVersions = Config.getBuildVersions()
        return buildVersions.getRepoEventVersion(gitRepo.getRemotePath(), config.gitTagPrefix, promotion.promoteToEvent)
    }

    /**
     * Get the next semVer to promote to
    **/
    SemVer getNextSemVer(String tagType) {
        def nextSemVer = currSemVer?.copy()
        // Return the user defined version if it is set, else get nextSemVer like usual
        if (promoteToVersion) {
            SemVer promoteToSemVer = new SemVer(promoteToVersion)

            // Check if there are any reasons not to write to build versions
            //   1. If this is a release event but our override is a prerelease
            //   2. If this is a prerelease event but our override is a release
            //   3. If this tag < the current tag
            if (!promoteToSemVer.isValid ||
                (tagType == "release" && promoteToSemVer.isPreRelease) ||
                (tagType != "release" && !promoteToSemVer.isPreRelease) ||
                (nextSemVer && nextSemVer.compareTo(promoteToSemVer) > 0)) {
                writebackBuildVersions = false

                // If we are not writing back to build versions (version less than current version),
                //   then don't emit any events (won't have the right version anyways)
                emitEvents = []
            }

            return promoteToSemVer
        }

        def buildVersions = Config.getBuildVersions()
        // If nextSemVer doesn't exist or its version without prerelease is smaller than baseSemVer, use baseSemVer
        if (nextSemVer == null || !nextSemVer.isValid || (baseSemVer.compareTo(nextSemVer.copyNoPrerelease()) > 0)) {
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
     * Set versionChange instance var
    **/
    void setVersionChange(String gitCommitToTag, String gitTagToTag) {
        SemVer currSemVer = getCurrSemVer()
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
            if (semVerReferenceHash &&
                !promoteToVersion && (
                    semVerReferenceHash == gitCommitToTag ||
                    semVerReferenceHash == gitRepo.getTagReferenceHash(fullGitTagToTag))) {
                versionChange = false
                Config.pipeline.echo "No version changes detected"
            }
        }
    }

    /**
     * Print out each of the images being promoted
    **/
    void prePromoteMessages(String promoteVersionString) {
        imageSummary = "Images"
        config.images.each { image ->
            Config.pipeline.echo "Promoting '${image.path}' from '${image.tag}' to '${promoteVersionString}'"
            notifySuccessMessage += "\n${image.path} promoted from '${image.tag}' to '${promoteVersionString}'"
        }
    }

    /**
     * If not an automated run, wait for user to click the button
    **/
    void waitForUserConfirmation(String promoteVersionString) {
        if (!trigger) {
            Config.pipeline.timeout(time: 10, unit: 'MINUTES') {
                Config.pipeline.input "Upgrade images to version '${promoteVersionString}'?"
            }
        }
    }

    /**
     * Iterate through images and retag and push to new registry. Write to build versions if not a user-defined promotion version
    **/
    void retagImages(String pushEvent, String promoteVersionString, SemVer nextSemVer) {
        def buildVersions = Config.getBuildVersions()
        def pushRegistries = config.getEventRegistries(pushEvent)
        if (pushRegistries.size() == 0) {
            Config.pipeline.error "'config.eventRegistryKeys' must specify a registry for event '${pushEvent}'"
        }

        def refTag = "tag-${Utils.alphaNumericDashLower(pushEvent.substring("tag/".length()))}"
        if (refTag == "tag-release") {
            refTag = "latest"
        }

        config.images.each { image ->
            promoteFromRegistry.withCredentials {
                image.pull()
            }

            imageSummary += "\n${image.path} promoted"
            imageSummary += "\n\tfrom ${formatImageSummary(image, promotion.event, promoteFromRegistry)}"

            pushRegistries.each { pushRegistry ->
                def newImageSemVer = image.copy()
                newImageSemVer.host = pushRegistry.host
                newImageSemVer.namespace = pushRegistry.namespace
                newImageSemVer.tag = promoteVersionString

                def newImageRef

                // Don't push a reftag if we are not writing back
                if (writebackBuildVersions) {
                    newImageRef = image.copy()
                    newImageRef.tag = refTag
                    image.reTag(newImageRef)
                }

                image.reTag(newImageSemVer)

                pushRegistry.withCredentials {
                    newImageSemVer.push()
                    if (newImageRef) {
                        newImageRef.push()
                    }
                }

                imageSummary += "\n\tto   ${formatImageSummary(newImageSemVer, promotion.promoteToEvent, pushRegistry)}"

                if (newImageRef) {
                    imageSummary += "\n\t     ${formatImageSummary(newImageRef, promotion.promoteToEvent, pushRegistry)}"
                }
            }
            if (writebackBuildVersions) {
                buildVersions.setEventImageVersion(pushEvent, image, promoteVersionString)
            }
        }
        if (writebackBuildVersions) {
            buildVersions.setRepoEventVersion(gitRepo.getRemotePath(), config.gitTagPrefix, pushEvent, nextSemVer)
        }
    }

    /**
     * Determine promotion type (release/non-release) and retag images
    **/
    void promoteImages(String tagType, String promoteVersionString, SemVer nextSemVer) {
        def buildVersions = Config.getBuildVersions()
        if (tagType == "release") {
            config.promotionMap.keySet().toList().each { k ->
                def v = config.promotionMap[k]
                if (v.promoteToEvent?.startsWith("tag/")) {
                    def currentSemVer = buildVersions.getRepoEventVersion(gitRepo.getRemotePath(), config.gitTagPrefix, v.promoteToEvent)
                    if (nextSemVer > currentSemVer) {
                        emitEvents.add(v.promoteToEvent)
                        retagImages(v.promoteToEvent, promoteVersionString, nextSemVer)
                    }
                }
            }
        } else {
            retagImages(promotion.promoteToEvent, promoteVersionString, nextSemVer)
        }
    }

    void saveBuildVersions() {
        def buildVersions = Config.getBuildVersions()
        buildVersions.save()
    }

    void tagGitRepo(String gitCommitToTag, String gitTagToTag, String promoteVersionString) {
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
            def gitTag = promoteVersionString
            if (config.gitTagPrefix) {
                gitTag = "${config.gitTagPrefix}${gitTag}"
            }
            gitRepo.tagAndPush(gitTag)
        }
    }

    def promote() {
        def tagType = getTagType()

        List<String> gitTagInfo = getGitCommit()
        String gitCommitToTag = gitTagInfo[0]
        String gitTagToTag = gitTagInfo[1]

        SemVer nextSemVer
        String promoteVersionString


        nextSemVer = getNextSemVer(tagType)
        promoteVersionString = nextSemVer.toString()
        setVersionChange(gitCommitToTag, gitTagToTag)

        if (versionChange) {
            prePromoteMessages(promoteVersionString)
            waitForUserConfirmation(promoteVersionString)

            promoteImages(tagType, promoteVersionString, nextSemVer)
            saveBuildVersions()
            tagGitRepo(gitCommitToTag, gitTagToTag, promoteVersionString)
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
