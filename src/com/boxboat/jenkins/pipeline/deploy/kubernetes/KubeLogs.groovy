package com.boxboat.jenkins.pipeline.deploy.kubernetes

import com.boxboat.jenkins.library.LibraryScript
import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config

class KubeLogs {

    static class PollParams extends BaseConfig<PollParams> {
        String outFile
        String namespace
        String labels
    }

    static def poll(Map paramsMap) {
        Config.pipeline.sh pollScript(paramsMap)
    }

    static String pollScript(Map paramsMap) {
        def params = new PollParams().newFromObject(paramsMap)

        if (!params.outFile) {
            Config.pipeline.error "'outFile' is required"
        }

        def namespaceArg = params.namespace ? "-n \"${params.namespace}\"" : ""
        def labelsArg = params.labels ? "-l \"${params.labels}\"" : ""

        return """
            ${LibraryScript.run("pod-logs.sh")} -o "${params.outFile}" ${namespaceArg} ${labelsArg}
        """.trim()
    }

}
