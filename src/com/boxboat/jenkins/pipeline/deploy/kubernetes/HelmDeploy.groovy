package com.boxboat.jenkins.pipeline.deploy.kubernetes

import com.boxboat.jenkins.library.config.Config

class HelmDeploy implements Serializable {

    public String chart

    public String directory = "."

    public String name

    Map<String, Object> options = [:]

    /**
     * Delete helm deployment
     */
    public delete() {
        Config.pipeline.sh deleteScript()
    }

    public deleteScript() {
        return """
            helm delete ${name} --purge
        """
    }

    /**
     * Update `helm dependency build` based on requirements.lock
     */
    public dependencyBuild() {
        Config.pipeline.sh dependencyBuildScript()
    }

    public dependencyBuildScript() {
        return """
            helm_current_dir=\$(pwd)
            cd "${directory}"
            helm dependency build
            cd "\$helm_current_dir"
        """
    }

    public install(Map<String, Object> additionalOptions = [:]) {
        Config.pipeline.sh installScript(additionalOptions)
    }

    public installScript(Map<String, Object> additionalOptions = [:]) {
        def combinedOptions = combineOptions(options, additionalOptions)
        combinedOptions["name"] = [name]
        return """
            helm_current_dir=\$(pwd)
            cd "${directory}"
            helm install ${optionsString(combinedOptions)} "${chart}"
            cd "\$helm_current_dir"
        """
    }

    public upgrade(Map<String, Object> additionalOptions = [:]) {
        Config.pipeline.sh upgradeScript(additionalOptions)
    }

    public upgradeScript(Map<String, Object> additionalOptions = [:]) {
        def combinedOptions = combineOptions(options, additionalOptions)
        return """
            helm_current_dir=\$(pwd)
            cd "${directory}"
            helm upgrade ${optionsString(combinedOptions)} "${name}" "${chart}"
            cd "\$helm_current_dir"
        """
    }

    private static Map<String, List<String>> combineOptions(Map<String, Object> options1, Map<String, Object> options2) {
        Map<String, List<String>> combinedOptions = [:]
        def combineCl = { String k, v ->
            if (!(v instanceof List)) {
                v = [v]
            }
            combinedOptions[k] = combinedOptions.get(k, []) + v
        }
        options1.each(combineCl)
        options2.each(combineCl)
        return combinedOptions
    }

    private static optionsString(Map<String, Object> allOptions) {
        return allOptions.collect { k, v ->
            def optionSwitch = k.length() == 1 ? "-${k}" : "--${k}"
            if (!(v instanceof List)) {
                v = [v]
            }
            return v.collect { optionValue ->
                if (optionValue instanceof Boolean && optionValue) {
                    return "$optionSwitch"
                }
                optionValue = optionValue.toString()
                return "$optionSwitch \"${optionValue.replace('"', '\\"')}\""
            }.join(" ")
        }.join(" ")
    }

}
