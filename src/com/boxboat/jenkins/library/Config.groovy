package com.boxboat.jenkins.library

import com.boxboat.jenkins.library.deployTarget.IDeployTarget
import com.boxboat.jenkins.library.docker.Registry
import com.boxboat.jenkins.library.git.GitConfig
import com.boxboat.jenkins.library.notification.NotificationsConfig

@Grab('org.yaml:snakeyaml:1.19')
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor

@Grab('org.apache.commons:commons-lang3:3.7')
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

class Config implements Serializable {

    static Config Config = new Config()

    static Config CreateConfig(String yamlStr) {
        Yaml yaml = new Yaml(new CustomClassLoaderConstructor(Config.class.classLoader))
        return (Config) yaml.loadAs(yamlStr, Config.class)
    }

    static void LoadConfig(String yamlStr) {
        Config = CreateConfig(yamlStr)
    }

    Map<String, IDeployTarget> deployTargetMap = [:]

    GitConfig git = new GitConfig()

    NotificationsConfig notifications = new NotificationsConfig()

    Map<String, Registry> registryMap = [:]

    Map<String, Vault> vaultMap = [:]

    IDeployTarget getDeployTarget(String key) {
        def deploymentTarget = deployTargetMap.get(key)
        if (!deploymentTarget) {
            throw new Exception("deployTarget entry '${key}' does not exist in config file")
        }
        return deploymentTarget
    }

    Registry getRegistry(String key) {
        def registry = registryMap.get(key)
        if (!registry) {
            throw new Exception("registry entry '${key}' does not exist in config file")
        }
        return registry
    }

    Vault getVault(String key) {
        def vault = vaultMap.get(key)
        if (!vault) {
            throw new Exception("vault entry '${key}' does not exist in config file")
        }
        return vault
    }

    @Override
    boolean equals(Object o) {
        if (!(o instanceof Config)) {
            return false
        }
        Config m = (Config) o

        return new EqualsBuilder()
                .append(this.git, m.git)
                .append(this.notifications, m.notifications)
                .append(this.registryMap, m.registryMap)
                .append(this.vaultMap, m.vaultMap)
                .isEquals()
    }

    @Override
    int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.git)
                .append(this.notifications)
                .append(this.registryMap)
                .append(this.vaultMap)
                .toHashCode()
    }

}
