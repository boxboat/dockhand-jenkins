package com.boxboat.jenkins.pipeline.deploy

import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.buildVersions.GitBuildVersions
import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.config.DeployConfig
import com.boxboat.jenkins.library.deploy.DeployType
import com.boxboat.jenkins.library.deploy.Deployment
import com.boxboat.jenkins.library.deployTarget.IDeployTarget
import com.boxboat.jenkins.library.docker.Image
import com.boxboat.jenkins.library.environment.Environment
import com.boxboat.jenkins.pipeline.BoxBase

class BoxDeploy extends BoxBase<DeployConfig> {

    protected DeployType deployType
    protected IDeployTarget deployTarget
    protected Environment environment
    protected Deployment deployment

    BoxDeploy(Map config = [:]) {
        super(config)
    }

    @Override
    protected String configKey() {
        return "deploy"
    }

    def init() {
        super.init()
        if (config.deployTargetKey) {
            deployType = DeployType.DeployTarget
        } else if (config.environmentKey) {
            deployType = DeployType.Environment
        } else if (config.deploymentKey) {
            deployType = DeployType.Deployment
        } else {
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
                    if (!this.event) {
                        Config.pipeline.error "'event' must be set for this deployment"
                    }
                    this.event = Utils.cleanEvent(this.event)
                    this.eventMatcher = event =~ deployment.eventRegex
                    if (!eventMatcher.matches()) {
                        Config.pipeline.error "event '${this.event}' does not match deployment.eventRegex '${deployment.eventRegex}'"
                    }
                    deployment.event = this.event
                }
            case DeployType.Environment:
                environment = Config.global.getEnvironment(config.environmentKey)
                config.deployTargetKey = environment.deployTargetKey
            case DeployType.DeployTarget:
                deployTarget = Config.global.getDeployTarget(config.deployTargetKey)
        }
    }

    static class ImageTagsParams extends BaseConfig<ImageTagsParams> {
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

        def buildVersions = new GitBuildVersions()
        buildVersions.checkout(gitAccount)
        Config.pipeline.sh """
            rm -f "${params.outFile}"
        """
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
            if (buildVersions.writeEventImageVersion(event, image, params.outFile, params.format)) {
                return
            }
            String triedEvents = event
            if (eventFallback) {
                if (buildVersions.writeEventImageVersion(event, image, params.outFile, params.format)) {
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

}
