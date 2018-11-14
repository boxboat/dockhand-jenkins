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

        // copy the shared library
        String sharedLibraryZip = steps.libraryResource(resource: 'com/boxboat/jenkins/sharedLibraryScripts.zip', encoding: "Base64")
        steps.writeFile(file: "sharedLibraryScripts.zip", text: sharedLibraryZip, encoding: "Base64")
        steps.sh """
            rm -rf sharedLibraryScripts
            mkdir sharedLibraryScripts
            unzip sharedLibraryScripts.zip -d sharedLibraryScripts
            find sharedLibraryScripts -type f -name "*.sh" -exec chmod +x {} \\;
        """
    }

    def cleanup() {
        gitRepo?.resetAndClean()
    }

}
