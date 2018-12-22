package com.boxboat.jenkins.library.config

import com.boxboat.jenkins.library.deployTarget.IDeployTarget
import com.boxboat.jenkins.library.docker.Registry
import com.boxboat.jenkins.library.environment.Environment
import com.boxboat.jenkins.library.git.GitConfig
import com.boxboat.jenkins.library.notify.INotifyTarget
import com.boxboat.jenkins.library.vault.Vault

class GlobalConfig extends BaseConfig<GlobalConfig> implements Serializable {

    Map<String, IDeployTarget> deployTargetMap

    Map<String, Environment> environmentMap

    GitConfig git

    Map<String, INotifyTarget> notifyTargetMap

    Map<String, Registry> registryMap

    RepoConfig repo

    Map<String, Vault> vaultMap

    IDeployTarget getDeployTarget(String key) {
        def deployTarget = deployTargetMap.get(key)
        if (!deployTarget) {
            throw new Exception("deployTarget entry '${key}' does not exist in config file")
        }
        return deployTarget
    }

    Environment getEnvironment(String key) {
        def environment = environmentMap.get(key)
        if (!environment) {
            throw new Exception("environment entry '${key}' does not exist in config file")
        }
        return environment
    }

    INotifyTarget getNotifyTarget(String key) {
        def provider = notifyTargetMap.get(key)
        if (!provider) {
            throw new Exception("notifyTargetMap entry '${key}' does not exist in config file")
        }
        return provider
    }

    Registry getRegistry(String key) {
        def registry = registryMap.get(key)
        if (!registry) {
            throw new Exception("registryKey entry '${key}' does not exist in config file")
        }
        return registry
    }

    Vault getVault(String key) {
        def vault = vaultMap.get(key)
        if (!vault) {
            throw new Exception("vaultKey entry '${key}' does not exist in config file")
        }
        return vault
    }

}
