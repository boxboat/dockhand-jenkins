package com.boxboat.jenkins.pipeline

import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.DeployConfig
import com.boxboat.jenkins.library.config.GlobalConfig
import com.boxboat.jenkins.library.deploy.DeployType
import com.boxboat.jenkins.library.deploy.Deployment
import com.boxboat.jenkins.library.deployTarget.IDeployTarget
import com.boxboat.jenkins.library.docker.Image
import com.boxboat.jenkins.library.environment.Environment

class BoxDeploy extends BoxBase<DeployConfig> {

    protected DeployType deployType
    protected IDeployTarget deployTarget
    protected Environment environment
    protected Deployment deployment

    BoxDeploy(Map config) {
        super(config)
    }

    @Override
    protected String configKey() {
        return "deploy"
    }

    static def create(Map config) {
        def deploy = new BoxDeploy(config)
        return deploy
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
            steps.error "'deployTargetKey', 'environmentKey', or 'deploymentKey'  must be set"
        }
        //noinspection GroovyFallthrough
        switch (deployType) {
            case DeployType.Deployment:
                deployment = config.getDeployment(config.deploymentKey)
                config.environmentKey = deployment.environmentKey
            case DeployType.Environment:
                environment = GlobalConfig.config.getEnvironment(config.environmentKey)
                config.deployTargetKey = environment.deployTargetKey
            case DeployType.DeployTarget:
                deployTarget = GlobalConfig.config.getDeployTarget(config.deployTargetKey)
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
            steps.error "'outFile' is required"
        }
        if (!params.format) {
            params.format = Utils.fileFormatDetect(params.outFile)
        }
        params.format = Utils.fileFormatNormalize(params.format)
        if (params.format != "yaml") {
            steps.error "'format' is required and must be 'yaml'"
        }

        gitAccount.checkoutRepository(GlobalConfig.config.git.buildVersionsUrl, "build-versions", 1)
        steps.sh """
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
            def writeTagCl = { tryEvent ->
                def filePath = "build-versions/image-versions/${tryEvent}/${Utils.alphaNumericDashLower(image.path)}.yaml"
                def rc = steps.sh(returnStatus: true, script: """
                    if [ -f "${filePath}" ]; then
                        cat "$filePath" >> "${params.outFile}"
                        exit 0
                    fi
                    exit 1
                """)
                return rc == 0
            }
            if (writeTagCl(event)) {
                return
            }
            String triedEvents = event
            if (eventFallback) {
                if (writeTagCl(eventFallback)) {
                    return
                }
                triedEvents = "[${event}, ${eventFallback}]"
            }
            steps.error "build-versions does not contain a version for image '${image.path}', event: ${triedEvents}"
        }
        def yamlPathScript = Utils.yamlPathScript(params.yamlPath, params.outFile, params.format)
        if (yamlPathScript) {
            steps.sh yamlPathScript
        }
    }

    def withCredentials(closure) {
        deployTarget.withCredentials(steps, closure)
    }

}
