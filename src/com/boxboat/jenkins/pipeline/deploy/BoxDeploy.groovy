package com.boxboat.jenkins.pipeline.deploy

import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.config.DeployConfig
import com.boxboat.jenkins.library.deploy.DeployType
import com.boxboat.jenkins.library.deploy.Deployment
import com.boxboat.jenkins.library.deployTarget.IDeployTarget
import com.boxboat.jenkins.library.docker.Image
import com.boxboat.jenkins.library.environment.Environment
import com.boxboat.jenkins.library.trigger.Trigger
import com.boxboat.jenkins.pipeline.BoxBase

class BoxDeploy extends BoxBase<DeployConfig> implements Serializable {

    protected DeployType deployType
    protected IDeployTarget deployTarget
    protected Environment environment
    protected Deployment deployment
    protected String deployLink = "<link>"
    protected String imageSummary

    BoxDeploy(Map config = [:]) {
        super(config)
        setPropertiesFromMap(config)
    }

    @Override
    protected String configKey() {
        return "deploy"
    }

    @Override
    def init() {
        super.init()
        if (config.deployTargetKey) {
            deployType = DeployType.DeployTarget
            notifySuccessMessage = "Deployment to deploy target '${config.deployTargetKey}' succeeded"
            notifyFailureMessage = "Deployment to deploy target '${config.deployTargetKey}' failed"
        } else if (config.environmentKey) {
            deployType = DeployType.Environment
            notifySuccessMessage = "Deployment to environment '${config.environmentKey}' succeeded"
            notifyFailureMessage = "Deployment to environment '${config.environmentKey}' failed"
        } else if (config.deploymentKey) {
            deployType = DeployType.Deployment
            notifySuccessMessage = "Deployment '${config.deploymentKey}' succeeded"
            notifyFailureMessage = "Deployment '${config.deploymentKey}' failed"
        } else {
            // abort, since pipeline may refresh without any parameters
            Config.pipeline.currentBuild.result = 'ABORTED'
            Config.pipeline.error "'deployTargetKey', 'environmentKey', or 'deploymentKey'  must be set"
        }

        //noinspection GroovyFallthrough
        switch (deployType) {
            case DeployType.Deployment:
                deployment = config.getDeployment(config.deploymentKey)
                config.environmentKey = deployment.environmentKey
                if (!deployment.event) {
                    if (!deployment.eventRegex) {
                        Config.pipeline.error "'deployment.event' or 'deployment.eventRegex' must be set"
                    }
                    if (!this.triggerEvent) {
                        Config.pipeline.error "'triggerEvent' must be set for this deployment"
                    }
                    this.triggerEvent = Utils.cleanEvent(this.triggerEvent)
                    Boolean matches = false
                    def closure = {
                        def matcher = this.triggerEvent =~ deployment.eventRegex
                        matches = matcher.matches()
                        this.eventMatch = matcher.hasGroup() && matcher.size() > 0 ? matcher[0][1] : null
                    }
                    closure()
                    if (!matches) {
                        Config.pipeline.error "triggerEvent '${this.triggerEvent}' does not match deployment.eventRegex '${deployment.eventRegex}'"
                    }
                    deployment.event = this.triggerEvent
                }
                notifySuccessMessage = "Deployment '${config.deploymentKey}' for event '${deployment.event}' succeeded"
                notifyFailureMessage = "Deployment '${config.deploymentKey}' for event '${deployment.event}' failed"
                buildDescription = "${config.deploymentKey}"
            case DeployType.Environment:
                environment = Config.global.getEnvironment(config.environmentKey)
                config.deployTargetKey = environment.deployTargetKey
                if (!buildDescription) {
                    buildDescription = "${config.environmentKey}"
                }
            case DeployType.DeployTarget:
                deployTarget = Config.global.getDeployTarget(config.deployTargetKey)
                if (!buildDescription) {
                    buildDescription = "${config.deployTargetKey}"
                } else {
                    buildDescription += " (${config.deployTargetKey})"
                }
        }

        //<environment> - <branch name> - <last 12 commit hash> - Auto triggered || Triggered by user
        buildDescription += " - ${gitRepo.branch} - ${gitRepo.shortHash} - ${buildUser}"
    }

    @Override
    List<Trigger> triggers() {
        if (!config.images || !config.deploymentMap) {
            return []
        }
        String job = Config.pipeline.env.JOB_NAME
        def triggers = []
        config.deploymentMap.keySet().toList().each { deploymentKey ->
            def deployment = config.deploymentMap[deploymentKey]
            if (!deployment.trigger) {
                return
            }
            def params = [
                    [$class: 'StringParameterValue', name: 'deploymentKey', value: deploymentKey]
            ]
            Map<String, Boolean> imageMap = [:]
            def imageCl = { Image image ->
                if (!imageMap[image.path]) {
                    def trigger = new Trigger()
                    trigger.job = job
                    trigger.imagePaths = [image.path]
                    trigger.event = image.event
                    trigger.params = params
                    triggers.add(trigger)
                }
                imageMap[image.path] = true
            }
            deployment.imageOverrides.each(imageCl)
            config.imageOverrides.each(imageCl)
            def remainingImages = []
            config.images.each { image ->
                if (!imageMap[image.path]) {
                    remainingImages.add(image.path)
                    imageMap[image.path] = true
                }
            }
            if (remainingImages) {
                def trigger = new Trigger()
                trigger.job = job
                trigger.imagePaths = remainingImages
                trigger.event = deployment.event
                trigger.eventRegex = deployment.eventRegex
                trigger.params = params
                triggers.add(trigger)
            }
        }
        return triggers
    }

    static class ImageTagsParams extends BaseConfig<ImageTagsParams> implements Serializable {
        String format
        String outFile
        List<String> yamlPath
    }

    def writeImageTags(Map paramsMap) {
        ImageTagsParams params = (new ImageTagsParams()).newFromObject(paramsMap)
        if (!params.outFile) {
            Config.pipeline.error "'outFile' is required"
        }
        if (!params.format) {
            params.format = Utils.fileFormatDetect(params.outFile)
        }
        params.format = Utils.fileFormatNormalize(params.format)
        if (params.format != "yaml" && params.format != "env") {
            Config.pipeline.error "'format' is required and must be 'yaml'or 'env'"
        }

        def buildVersions = this.getBuildVersions()
        Config.pipeline.sh """
            rm -f "${params.outFile}"
        """
        imageSummary = "Images"
        config.images.each { Image image ->
            def event = deployment.event
            def eventFallback = deployment.eventFallback
            def imageOverridesCl = { Image imageOverride ->
                if (imageOverride.path == image.path) {
                    event = imageOverride.event
                    eventFallback = imageOverride.eventFallback
                }
            }
            config.imageOverrides.each imageOverridesCl
            deployment.imageOverrides.each imageOverridesCl
            if (Utils.isImageTagEvent(event)) {
                image.tag = Utils.imageTagFromEvent(event)
                buildVersions.writeImageVersion(image.tag, image, params.outFile, params.format)
                notifySuccessMessage += "\n${image.path} version: ${image.tag}"
                imageSummary += "\n${image.path}:${image.tag}"
                return
            } else if (buildVersions.writeEventImageVersion(event, image, params.outFile, params.format)) {
                image.tag = buildVersions.getEventImageVersion(event, image)
                notifySuccessMessage += "\n${image.path} version: ${image.tag}"
                config.getEventRegistries(event).each { registry ->
                    imageSummary += "\n${formatImageSummary(registry, image)}"
                }
                return
            }
            String triedEvents = event
            if (eventFallback) {
                if (Utils.isImageTagEvent(eventFallback)) {
                    image.tag = Utils.imageTagFromEvent(eventFallback)
                    buildVersions.writeImageVersion(image.tag, image, params.outFile, params.format)
                    notifySuccessMessage += "\n${image.path} version: ${image.tag}"
                    imageSummary += "\n${image.path}:${image.tag}"
                    return
                } else if (buildVersions.writeEventImageVersion(eventFallback, image, params.outFile, params.format)) {
                    image.tag = buildVersions.getEventImageVersion(eventFallback, image)
                    notifySuccessMessage += "\n${image.path} version: ${image.tag}"
                    config.getEventRegistries(eventFallback).each { registry ->
                        imageSummary += "\n${formatImageSummary(registry, image)}"
                    }
                    return
                }
                triedEvents = "[${event}, ${eventFallback}]"
            }
            Config.pipeline.error "build-versions does not contain a version for image '${image.path}', event: ${triedEvents}"
        }
        def yamlPathScript = Utils.yamlPathScript(params.yamlPath, params.outFile, params.format)
        if (yamlPathScript) {
            Config.pipeline.sh yamlPathScript
        }
    }

    def withCredentials(closure) {
        deployTarget.withCredentials(closure)
    }

    def summary() {
        pipelineSummaryMessage = ""
        if (config.deploymentKey) {
            pipelineSummaryMessage += "Deployment: ${config.deploymentKey}\n"
        }
        if (config.environmentKey) {
            pipelineSummaryMessage += "Environment: ${config.environmentKey}\n"
        }
        if (config.deployTargetKey) {
            pipelineSummaryMessage += "Deploy Target: ${config.deployTargetKey}\n"
        }
        pipelineSummaryMessage += """Deployment URL: ${deployLink}

Built from ${gitRepo.branch}: ${gitRepo.shortHash}
Branch: ${gitRepo.branchUrl}
Commit: ${gitRepo.commitUrl}

${imageSummary}

${buildUser}
        """
        super.summary()
    }

}
