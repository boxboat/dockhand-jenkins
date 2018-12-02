package com.boxboat.jenkins.pipeline

import com.boxboat.jenkins.library.SecretScript
import com.boxboat.jenkins.library.config.*
import com.boxboat.jenkins.library.git.GitAccount
import com.boxboat.jenkins.library.git.GitRepo
import com.boxboat.jenkins.library.notification.INotifyTarget
import com.boxboat.jenkins.library.notification.NotificationType

abstract class BoxBase<T extends CommonConfigBase> {

    public T config

    protected initialConfig
    protected GitAccount gitAccount
    protected GitRepo gitRepo
    protected INotifyTarget notifySuccess
    protected INotifyTarget notifyFailure
    protected steps


    BoxBase(Map config) {
        def className = this.class.simpleName
        config.each { k, v ->
            switch (k) {
                case "steps":
                    steps = v
                    break
                case "config":
                    initialConfig = v
                    break
                default:
                    throw new Exception("${className} does not support property '${k}'")
            }
        }
        if (!steps) {
            throw new Exception("${className} should be initialized with ${className}(steps: this)")
        }
        gitAccount = new GitAccount(steps: config.steps)
    }

    protected String configKey() {
        return "common"
    }

    def init() {
        // load the global config
        String configYaml = steps.libraryResource('com/boxboat/jenkins/config.yaml')
        def globalConfig = GlobalConfig.create(configYaml)
        def globalConfigDefault = (new GlobalConfig()).newDefault()
        globalConfigDefault.merge(globalConfig)
        GlobalConfig.config = globalConfigDefault

        // create the config
        def configKey = configKey()
        config = globalConfigDefault.repo."$configKey".newDefault()
        if (configKey != "common") {
            config.merge(globalConfig.repo.common)
        }
        config.merge(globalConfig.repo."$configKey")

        // update from Git
        gitRepo = gitAccount.checkoutScm()
        steps.env.GIT_COMMIT_SHORT_HASH = gitRepo.shortHash

        // create directory for shared library
        steps.sh """
            rm -rf sharedLibraryScripts
            mkdir sharedLibraryScripts
        """

        // merge config from jenkins.yaml if exists
        def configFile = steps.sh(returnStdout: true, script: """
            set +x
            for f in "jenkins.yml" "jenkins.yaml"; do
                if [ -f "\$f" ]; then
                    echo  "\$f"
                    break
                fi
            done
        """)?.trim()
        if (configFile) {
            String configFileContents = steps.readFile(configFile)
            def configFileObj = RepoConfig.create(configFileContents)
            if (configKey != "common") {
                config.merge(configFileObj.common)
            }
            config.merge(configFileObj."$configKey")
        }

        // merge the initial config
        if (initialConfig) {
            def initialConfigObj = config.newFromObject(initialConfig)
            config.merge(initialConfigObj)
        }

        if (config.notifyTargetKeySuccess) {
            notifySuccess = GlobalConfig.config.getNotifyTarget(config.notifyTargetKeySuccess)
        }
        if (config.notifyTargetKeyFailure) {
            notifyFailure = GlobalConfig.config.getNotifyTarget(config.notifyTargetKeyFailure)
        }
    }

    def success() {
        notifySuccess?.postMessage(steps, "Build Succeeded", NotificationType.SUCCESS)
    }

    def failure(Exception ex) {
        notifyFailure?.postMessage(steps, "Build Failed", NotificationType.FAILURE)
    }

    def cleanup() {
        gitRepo?.resetAndClean()
    }

    def secretReplaceScript(Map paramsMap) {
        SecretScript.replace(steps, paramsMap, config)
    }

    def secretFileScript(Map paramsMap) {
        SecretScript.file(steps, paramsMap, config)
    }

}
