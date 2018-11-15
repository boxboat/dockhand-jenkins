package com.boxboat.jenkins.pipeline

import com.boxboat.jenkins.library.git.GitAccount
import com.boxboat.jenkins.library.git.GitRepo
import com.boxboat.jenkins.library.notification.INotificationProvider
import com.boxboat.jenkins.library.notification.NotificationType

import static com.boxboat.jenkins.library.Config.LoadConfig
import static com.boxboat.jenkins.library.Config.Config

abstract class BoxBase {

    public steps
    public boolean notifySuccessDisable = false
    public boolean notifyFailureDisable = false
    public String notifySuccessProvider = ""
    public String notifyFailureProvider = ""

    protected GitAccount gitAccount
    protected GitRepo gitRepo
    protected INotificationProvider notifySuccess
    protected INotificationProvider notifyFailure

    BoxBase(Map config) {
        gitAccount = new GitAccount(steps: config.steps)
    }

    def init() {
        // load the config
        String configYaml = steps.libraryResource('com/boxboat/jenkins/config.yaml')
        LoadConfig(configYaml)

        // update from Git
        gitRepo = gitAccount.checkoutScm()
        steps.env.GIT_COMMIT_SHORT_HASH = gitRepo.shortHash

        // create directory for shared library
        steps.sh """
            rm -rf sharedLibraryScripts
            mkdir sharedLibraryScripts
        """

        if (!notifySuccessDisable) {
            if (notifySuccessProvider) {
                notifySuccess = Config.notifications.getProvider(notifySuccessProvider)
            } else if (Config.notifications.successProvider) {
                notifySuccess = Config.notifications.getProvider(Config.notifications.successProvider)
            }
        }
        if (!notifyFailureDisable) {
            if (notifyFailureProvider) {
                notifyFailure = Config.notifications.getProvider(notifyFailureProvider)
            } else if (Config.notifications.failureProvider) {
                notifyFailure = Config.notifications.getProvider(Config.notifications.failureProvider)
            }
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

}
