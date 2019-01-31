package com.boxboat.jenkins.pipeline.deploy.kubernetes

import com.boxboat.jenkins.library.LibraryScript
import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config

class KubePod implements Serializable {

    static class PollParams extends BaseConfig implements Serializable {
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

    static class ExecParams extends BaseConfig implements Serializable {
        String namespace
        String labels
        String container
        List<String> command
    }

    static def exec(Map paramsMap) {
        Config.pipeline.sh execScript(paramsMap)
    }

    static String execScript(Map paramsMap) {
        def params = new ExecParams().newFromObject(paramsMap)

        if (!params.command) {
            Config.pipeline.error "'command' is required"
        }

        def namespaceArg = params.namespace ? "-n \"${params.namespace}\"" : ""
        def labelsArg = params.labels ? "-l \"${params.labels}\"" : ""
        def containerArg = params.container ? "-c \"${params.container}\"" : ""

        return """
            ${LibraryScript.run("pod-exec.sh")} ${namespaceArg} ${labelsArg} ${containerArg} "${params.command.join('" "')}"
        """.trim()
    }

}
