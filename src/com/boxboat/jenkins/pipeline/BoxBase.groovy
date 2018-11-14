package com.boxboat.jenkins.pipeline

import com.boxboat.jenkins.library.git.GitAccount
import com.boxboat.jenkins.library.git.GitRepo
import com.boxboat.jenkins.library.Config

abstract class BoxBase {

    public steps

    protected GitAccount gitAccount
    protected GitRepo gitRepo

    BoxBase(Map config) {
        gitAccount = new GitAccount(steps: config.steps)
    }

    def init() {
        // load the config
        String configYaml = steps.libraryResource('com/boxboat/jenkins/config.yaml')
        Config.loadConfig(configYaml)

        // update from Git
        gitRepo = gitAccount.checkoutScm()

        // create directory for shared library
        steps.sh """
            rm -rf sharedLibraryScripts
            mkdir sharedLibraryScripts
        """
    }

    def cleanup() {
        gitRepo?.resetAndClean()
    }

}
