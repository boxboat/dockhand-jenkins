package com.boxboat.jenkins.pipeline

import com.boxboat.jenkins.library.SecretScript

import static com.boxboat.jenkins.library.Config.Config

abstract class BoxDeployBase extends BoxBase {

    public String vaultConfig = ""

    BoxDeployBase(Map config) {
        super(config)
    }

    def secretReplaceScript(List<String> globs, Map<String,String> env = [:]) {
        SecretScript.replace(steps, Config.getVault(vaultConfig), globs, env)
    }

    def secretFileScript(List<String> vaultKeys, String outFile, String format = "", boolean append = false) {
        SecretScript.file(steps, Config.getVault(vaultConfig), vaultKeys, outFile, format, append)
    }
}
