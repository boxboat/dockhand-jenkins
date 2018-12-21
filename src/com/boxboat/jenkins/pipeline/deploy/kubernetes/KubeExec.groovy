package com.boxboat.jenkins.pipeline.deploy.kubernetes

import com.boxboat.jenkins.library.LibraryScript
import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config

class KubeExec implements Serializable {

    static class ExecParams extends BaseConfig<ExecParams> implements Serializable {
        String namespace
        String labels
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

        return """
            ${LibraryScript.run("pod-exec.sh")} ${namespaceArg} ${labelsArg} "${params.command.join('" "')}"
        """.trim()
    }

}
