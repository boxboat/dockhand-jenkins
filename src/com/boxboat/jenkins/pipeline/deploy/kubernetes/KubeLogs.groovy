package com.boxboat.jenkins.pipeline.deploy.kubernetes

import com.boxboat.jenkins.library.LibraryScript
import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config

class KubeLogs implements Serializable {

    static class PollParams extends BaseConfig<PollParams> implements Serializable {
        String outFile
        String namespace
        String labels
        String container
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
        def containerArg = params.container ? "-c \"${params.container}\"" : ""

        return """
            ${LibraryScript.run("pod-logs.sh")} -o "${params.outFile}" ${namespaceArg} ${labelsArg} ${containerArg}
        """.trim()
    }

}
