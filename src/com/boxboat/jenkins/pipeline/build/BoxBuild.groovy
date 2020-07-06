package com.boxboat.jenkins.pipeline.build

import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.buildVersions.GitBuildVersions
import com.boxboat.jenkins.library.config.BuildConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.docker.Compose
import com.boxboat.jenkins.library.docker.Image
import com.boxboat.jenkins.library.docker.Registry
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

    String getEvent(){
        return "commit/${gitRepo.getBranch()}"
    }

    String getBuildTag(){
        return "build-${gitRepo.shortHash}".toString()
    }

    /**
     * Create list of tags to push
    **/
    List<String> getTags(Boolean isBranchTip, List<String> additionalImageTags = []) {
        def tags = [getBuildTag()] + additionalImageTags
        if(isBranchTip){
            tags.add(Utils.cleanTag(event))
        }
        return tags.unique()
    }

    /**
     * Iterate through each image tag and push to the registry
    **/
    void pushTags(Registry registry, Image image, List<String> tags) {
        tags.each { String tag ->
            def newImage = image.copy()
            newImage.host = registry.host
            newImage.namespace = registry.namespace
            newImage.tag = tag
            image.reTag(newImage)
            newImage.push()
            image.summary += "\n${formatImageSummary(newImage, event, registry)}"
        }
    }

    /**
     * Iterate through each image and push its tags
    **/
    void pushToRegistry(Registry registry, List<String> tags){
        config.images.each { Image image ->
            pushTags(registry, image, tags)
        }
    }

    /**
     * Iterate through each registry, login and pushed each iamge
    **/
    void loginAndPush(List<Registry> registries, List<String> tags){
        registries.each { registry ->
            registry.withCredentials {
                pushToRegistry(registry, tags)
            }
        }
    }

    /**
     * Push images, return true if registries exist (images pushed), false otherwise
    **/
    Boolean pushAllImages(List<String> tags){
        def pushedImages = false
        def registries = config.getEventRegistries(event)
        if (registries) {
            loginAndPush(registries, tags)
            pushedImages = true
        }
        return pushedImages
    }

    /**
     * Write updates to build versions repo
    **/
    void updateBuildVersions(){
        def buildVersions = Config.getBuildVersions()
        def repoPath = this.gitRepo.getRemotePath()
        config.images.each { image ->
            buildVersions.setEventImageVersion(event, image, getBuildTag())
            buildVersions.setImageRepoPath(image, repoPath)
        }
        buildVersions.save()
    }
    /**
     * Add any pipeline triggers based on the current event and write back to build-versions
    **/
    void branchTipEvent(){
        Config.pipeline.echo "isBranchTip: ${event}"
        emitEvents.add(event)

        updateBuildVersions()
    }

    /**
     * Determine the current event and tags to push, push image tags to registries.
     * On push from the tip of the breanch: emit events and write to build-versions
    **/
    void push(List<String> additionalImageTags = []) {
        Config.pipeline.echo gitRepo.getBranch()

        // Determine if this commit is at the tip of its branch. Expensive, so we pass this around
        def isBranchTip = gitRepo.isBranchTip()

        // Get all the tags to be applied to images
        def tags = getTags(isBranchTip, additionalImageTags)

        // Push the iamges to configured repos
        def pushedImages = pushAllImages(tags)

        // Write a summary of what was pushed
        formatImageSummary()

        // If we're at the head of the branch, store version and determine events to emit
        if (pushedImages && isBranchTip) {
            branchTipEvent()
        }
    }

    /**
     * Update imageSummary global variable with image info
    **/
    def formatImageSummary(){
        imageSummary = imageSummaryHeader()
        config.images.each { Image image ->
            imageSummary += image.summary
        }
    }

    /**
     * Get string representation of builds triggered by this pipeline
    **/
    String getTriggeredBuilds(){
        String triggeredBuilds = ""

        if (emitBuilds) {
            for (build in emitBuilds) {
                triggeredBuilds += "\n${build}"
            }
            triggeredBuilds += "\n"
        }
        return triggeredBuilds
    }

    /**
     * Gather info for summary message
    **/
    String buildSummaryMessage(){
        return """
Build for branch '${gitRepo.branch}' commit '${gitRepo.shortHash}'
Branch: ${gitRepo.branchUrl}
Commit: ${gitRepo.commitUrl}

${imageSummary}
${triggeredBuilds}
${buildUser}
        """
    }

    def summary() {
        pipelineSummaryMessage = buildSummaryMessage()
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
