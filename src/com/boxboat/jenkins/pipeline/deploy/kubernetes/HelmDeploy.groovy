package com.boxboat.jenkins.pipeline.deploy.kubernetes

import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.Config

class HelmDeploy implements Serializable {

    public String chart

    public String directory = "."

    public String name

    public String namespace

    Map<String, Object> options = [:]

    private int _majorVersion = 0

    public int majorVersion() {
        if (_majorVersion == 0) {
            _majorVersion = Utils.resultOrTest(Config.pipeline.sh(returnStdout: true, script: """
                if ! version=\$(helm version --client --short 2>/dev/null); then
                    version=\$(helm version --short)
                fi
                echo "\$version" | grep -oE '[0-9]+' | head -n 1
            """)?.trim()?.toInteger(), 3) as int
        }
        return _majorVersion
    }

    public testMajorVersion(int version) {
        _majorVersion = version
    }

    /**
     * Delete/Uninstall helm deployment
     */
    public delete(Map<String, Object> deleteOptions = [:]) {
        Config.pipeline.sh deleteScript()
    }

    public deleteScript(Map<String, Object> deleteOptions = [:]) {
        def combinedOptions = combineOptions(deleteOptions, [:])
        def command = ""
        Map<String, Object> additionalOptions = [:]
        if (majorVersion() <= 2) {
            command = "delete"
            combinedOptions["purge"] = true
        } else {
            command = "uninstall"
            if (namespace) {
                combinedOptions["namespace"] = namespace
            }
        }
        return """
            helm ${command} ${optionsString(combinedOptions)} "${name}"
        """
    }

    public uninstall(Map<String, Object> uninstallOptions = [:]) {
        delete(uninstallOptions)
    }

    public uninstallScript(Map<String, Object> uninstallOptions = [:]) {
        deleteScript(uninstallOptions)
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
        if (namespace) {
            combinedOptions["namespace"] = namespace
        }
        def namePositional = ""
        if (majorVersion() <= 2) {
            combinedOptions["name"] = name
        } else {
            namePositional = "\"${name}\""
        }
        return """
            helm_current_dir=\$(pwd)
            cd "${directory}"
            helm install ${optionsString(combinedOptions)} ${namePositional} "${chart}"
            cd "\$helm_current_dir"
        """
    }

    public upgrade(Map<String, Object> additionalOptions = [:]) {
        Config.pipeline.sh upgradeScript(additionalOptions)
    }

    public upgradeScript(Map<String, Object> additionalOptions = [:]) {
        def combinedOptions = combineOptions(options, additionalOptions)
        if (namespace) {
            combinedOptions["namespace"] = namespace
        }
        return """
            helm_current_dir=\$(pwd)
            cd "${directory}"
            helm upgrade ${optionsString(combinedOptions)} "${name}" "${chart}"
            cd "\$helm_current_dir"
        """
    }

    private static Map<String, Object> combineOptions(Map<String, Object> options1, Map<String, Object> options2) {
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
