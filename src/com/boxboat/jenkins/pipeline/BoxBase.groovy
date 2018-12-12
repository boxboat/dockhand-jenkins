package com.boxboat.jenkins.pipeline

import com.boxboat.jenkins.library.config.CommonConfigBase
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.config.GlobalConfig
import com.boxboat.jenkins.library.config.RepoConfig
import com.boxboat.jenkins.library.git.GitAccount
import com.boxboat.jenkins.library.git.GitRepo
import com.boxboat.jenkins.library.notification.INotifyTarget
import com.boxboat.jenkins.library.notification.NotificationType

import java.lang.reflect.Modifier
import java.util.regex.Matcher

abstract class BoxBase<T extends CommonConfigBase> implements Serializable {

    public T config

    public boolean trigger
    public String event
    public Matcher eventMatcher
    public String overrideBranch
    public String overrideCommit

    protected initialConfig
    protected GitAccount gitAccount
    protected GitRepo gitRepo
    protected INotifyTarget notifySuccess
    protected INotifyTarget notifyFailure

    BoxBase(Map<String, Object> config = [:]) {
        config.keySet().toList().each { k ->
            def v = config[k]
            switch (k) {
                case "pipeline":
                    Config.pipeline = v
                    break
                case "config":
                    initialConfig = v
                    break
                default:
                    def property = this.metaClass.getMetaProperty(k)
                    if (property && Modifier.isPublic(property.modifiers) && !Modifier.isStatic(property.modifiers)) {
                        this."$k" = v
                    } else {
                        throw new Exception("${this.class.simpleName} does not support property '${k}'")
                    }
            }
        }
        gitAccount = new GitAccount()
    }

    protected String configKey() {
        return "common"
    }

    def wrap(Closure closure) {
        try {
            Config.pipeline = closure.thisObject
            Config.pipeline.stage("Initialize") {
                init()
            }
            closure()
            success()
        } catch (Exception ex) {
            failure(ex)
            throw ex
        } finally {
            Config.pipeline.stage("Cleanup") {
                cleanup()
            }
        }
    }

    def init() {
        // load the global config
        String configYaml = Config.pipeline.libraryResource('com/boxboat/jenkins/config.yaml')
        def globalConfig = new GlobalConfig().newFromYaml(configYaml)
        Config.global = new GlobalConfig().newDefault()
        Config.global.merge(globalConfig)

        // create the config
        def configKey = configKey()
        config = Config.global.repo."$configKey".newDefault()
        if (configKey != "common") {
            config.merge(globalConfig.repo.common)
        }
        config.merge(globalConfig.repo."$configKey")

        // update from Git
        gitRepo = gitAccount.checkoutScm()
        if (overrideBranch) {
            Config.pipeline.echo "Changing Branch to '${overrideBranch}'"
            gitRepo.fetchAndCheckoutBranch(overrideBranch)
        }
        if (overrideCommit) {
            if (!overrideBranch) {
                gitRepo.fetchAndCheckoutBranch(gitRepo.branch)
            }
            Config.pipeline.echo "Changing Commit to '${overrideCommit}'"
            if (!gitRepo.currentBranchContainsCommit(overrideCommit)) {
                Config.pipeline.error "Current branch '${gitRepo.branch}' not include override commit '${overrideCommit}'"
            }
            gitRepo.resetToHash(overrideCommit)
        }
        Config.pipeline.env.GIT_COMMIT_SHORT_HASH = gitRepo.shortHash

        // create directory for shared library
        Config.pipeline.sh """
            rm -rf sharedLibraryScripts
            mkdir sharedLibraryScripts
        """

        // merge config from jenkins.yaml if exists
        def configFile = Config.pipeline.sh(returnStdout: true, script: """
            set +x
            for f in "jenkins.yml" "jenkins.yaml"; do
                if [ -f "\$f" ]; then
                    echo  "\$f"
                    break
                fi
            done
        """)?.trim()
        if (configFile) {
            String configFileContents = Config.pipeline.readFile(configFile)
            def configFileObj = new RepoConfig().newFromYaml(configFileContents)
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
        Config.repo = config

        if (config.notifyTargetKeySuccess) {
            notifySuccess = Config.global.getNotifyTarget(config.notifyTargetKeySuccess)
        }
        if (config.notifyTargetKeyFailure) {
            notifyFailure = Config.global.getNotifyTarget(config.notifyTargetKeyFailure)
        }
    }

    def success() {
        notifySuccess?.postMessage("Build Succeeded", NotificationType.SUCCESS)
    }

    def failure(Exception ex) {
        notifyFailure?.postMessage("Build Failed", NotificationType.FAILURE)
    }

    def cleanup() {
        gitRepo?.resetAndClean()
    }

}
