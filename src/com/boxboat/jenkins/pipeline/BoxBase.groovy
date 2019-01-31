package com.boxboat.jenkins.pipeline

import com.boxboat.jenkins.library.buildVersions.GitBuildVersions
import com.boxboat.jenkins.library.config.CommonConfigBase
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.config.GlobalConfig
import com.boxboat.jenkins.library.config.RepoConfig
import com.boxboat.jenkins.library.git.GitAccount
import com.boxboat.jenkins.library.git.GitRepo
import com.boxboat.jenkins.library.notify.INotifyTarget
import com.boxboat.jenkins.library.notify.NotifyType
import com.boxboat.jenkins.library.trigger.Trigger
import com.boxboat.jenkins.library.docker.Image
import com.boxboat.jenkins.library.registry.Registry
import com.cloudbees.groovy.cps.NonCPS

import java.lang.reflect.Modifier

abstract class BoxBase<T extends CommonConfigBase> implements Serializable {

    public T config

    public String globalConfigPath = "com/boxboat/jenkins/config.yaml"
    public Boolean trigger
    public String triggerEvent
    public String triggerImagePathsCsv
    public String buildUser = "Auto triggered"
    public String eventMatch
    public String overrideBranch
    public String overrideCommit
    public String buildDescription = ""
    public String notifySuccessMessage = "Build Succeeded"
    public String notifyFailureMessage = "Build Failed"
    public String pipelineSummaryMessage = "Build Succeeded"

    protected String failureSummary
    protected GitBuildVersions _buildVersions
    protected initialConfig
    protected GitAccount gitAccount = new GitAccount()
    protected GitRepo gitRepo
    protected emitEvents = []
    protected emitBuilds = []

    BoxBase(Map config = [:]) {
        setPropertiesFromMap(config)
    }

    @NonCPS
    protected setPropertiesFromMap(Map<String, Object> config) {
        config.each { k, v ->
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
    }

    protected String configKey() {
        return "common"
    }

    def wrap(Closure closure) {
        try {
            Config.pipeline = closure.thisObject
            Config.pipeline.stage("Initialize") {
                init()
                setDescription()
            }
            closure()
            Config.pipeline.stage("Summary") {
                runTriggers()
                success()
                summary()
            }
        } catch (Exception ex) {
            if (config) {
                failure(ex)
            }
            throw ex
        } finally {
            if (config) {
                Config.pipeline.stage("Cleanup") {
                    cleanup()
                }
            }
        }
    }

    def init() {
        // load the global config
        String configYaml = Config.pipeline.libraryResource(globalConfigPath)
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

        // set null properties to non-null params
        Config.pipeline.params.keySet().toList().each { k ->
            if (Config.pipeline.params[k] == null) {
                return
            }
            def property = this.metaClass.getMetaProperty("$k")
            if (property && Modifier.isPublic(property.modifiers) && !Modifier.isStatic(property.modifiers) && this."$k" == null) {
                this."$k" = Config.pipeline.params[k]
            }
        }

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

        Config.pipeline.currentBuild?.rawBuild?.getCauses()?.reverseEach { cause ->
            if (cause.getClass().getSimpleName() == "UserIdCause") {
                buildUser = "started by ${cause.getUserName()}"
            }
        }

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

        // set null top-level config properties to non-null params
        Config.pipeline.params.keySet().toList().each { k ->
            if (Config.pipeline.params[k] == null) {
                return
            }
            def configProperty = this.config.metaClass.getMetaProperty("$k")
            if (configProperty && Modifier.isPublic(configProperty.modifiers) && !Modifier.isStatic(configProperty.modifiers) && this.config."$k" == null) {
                this.config."$k" = Config.pipeline.params[k]
            }
        }

        // write triggers
        writeTriggers()
    }

    def setDescription() {
        if (buildDescription) {
            Config.pipeline.currentBuild.description = buildDescription
        }
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected notify(List<INotifyTarget> notifyTargets, String message, NotifyType notifyType) {
        notifyTargets.each { notifyTarget ->
            notifyTarget.postMessage(message, notifyType)
        }
    }

    def notifySuccess(String message) {
        notify(config.notify.successTargets(), message, NotifyType.SUCCESS)
    }

    def notifyFailure(String message) {
        notify(config.notify.failureTargets(), message, NotifyType.FAILURE)
    }

    def notifyInfo(String message) {
        notify(config.notify.infoTargets(), message, NotifyType.INFO)
    }

    def writeTriggers() {
        if (gitRepo.branch == config.defaultBranch && gitRepo.isBranchTip()) {
            def buildVersions = this.getBuildVersions()
            String job = Config.pipeline.env.JOB_NAME
            def triggers = Trigger.merge(this.triggers())
            if (triggers) {
                buildVersions.setJobTriggers(job, triggers)
            } else {
                buildVersions.removeJobTriggers(job)
            }
            buildVersions.save()
        }
    }

    def runTriggers() {
        if (emitEvents) {
            def buildVersions = this.getBuildVersions()
            def triggers = buildVersions.getAllJobTriggers()
            def matchingTriggers = Trigger.matches(emitEvents, config.images, triggers)
            matchingTriggers.each { trigger ->
                trigger.params += [
                        [$class: 'BooleanParameterValue', name: 'trigger', value: true],
                        [$class: 'StringParameterValue', name: 'triggerEvent', value: trigger.event],
                        [$class: 'StringParameterValue', name: 'triggerImagePathsCsv', value: trigger.imagePaths.join(",")]
                ]
                // Cannot get build number if wait:false
                //   https://issues.jenkins-ci.org/browse/JENKINS-41466
                Config.pipeline.build(job: "/${trigger.job}", parameters: trigger.params, wait: false)
                String jobInfo = "Triggered job: ${trigger.job}, with parameters: ${trigger.params}"
                emitBuilds.add(jobInfo)
            }
        }
    }

    protected List<Trigger> triggers() {
        return []
    }

    def getBuildVersions() {
        if (!_buildVersions) {
            _buildVersions = new GitBuildVersions()
            _buildVersions.checkout(gitAccount)
        }
        return _buildVersions
    }

    def success() {
        notifySuccess(notifySuccessMessage)
    }

    def summary() {
        pipelineSummaryMessage = (failureSummary) ? "${failureSummary}\n\n${pipelineSummaryMessage}" : pipelineSummaryMessage
        Config.pipeline.echo pipelineSummaryMessage
    }

    def failure(Exception ex) {
        failureSummary = "Run Aborted."
        if (Config.pipeline.currentBuild.result != 'ABORTED') {
            notifyFailureMessage += "\n${ex.getMessage()}"
            notifyFailure(notifyFailureMessage)
            failureSummary = "Error: ${ex}"
        }
    }

    String formatImageSummary(Registry registry, Image image) {
        String imageName = "${image.path}:${image.tag ?: "latest"}"
        String url = registry.getRegistryImageUrl(image.path, image.tag)
        imageName = String.format('%-60s', imageName)

        return "${imageName} ${url}"
    }

    def cleanup() {
        gitRepo?.resetAndClean()
    }

}
