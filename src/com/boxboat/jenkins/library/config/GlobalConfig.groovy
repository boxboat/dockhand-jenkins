package com.boxboat.jenkins.library.config

import com.boxboat.jenkins.library.aws.AwsProfile
import com.boxboat.jenkins.library.azure.AzureProfile
import com.boxboat.jenkins.library.deployTarget.IDeployTarget
import com.boxboat.jenkins.library.docker.Registry
import com.boxboat.jenkins.library.environment.Environment
import com.boxboat.jenkins.library.gcloud.GCloudAccount
import com.boxboat.jenkins.library.git.GitConfig
import com.boxboat.jenkins.library.notify.INotifyTarget
import com.boxboat.jenkins.library.vault.Vault

class GlobalConfig extends BaseConfig<GlobalConfig> implements Serializable {

    Map<String, AwsProfile> awsProfileMap

    Map<String, AzureProfile> azureProfileMap

    Map<String, IDeployTarget> deployTargetMap

    Map<String, Environment> environmentMap

    Map<String, GCloudAccount> gCloudAccountMap

    GitConfig git

    Map<String, INotifyTarget> notifyTargetMap

    Map<String, Registry> registryMap

    RepoConfig repo

    Map<String, Vault> vaultMap

    AwsProfile getAwsProfile(String key) {
        def awsProfile = awsProfileMap.get(key)
        if (!awsProfile) {
            throw new Exception("awsProfileKey entry '${key}' does not exist in config file")
        }
        return awsProfile
    }

    AzureProfile getAzureProfile(String key) {
        def azureProfile = azureProfileMap.get(key)
        if (!azureProfile) {
            throw new Exception("azureProfile entry '${key}' does not exist in config file")
        }
        return azureProfile

    }

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

    GCloudAccount getGCloudAccount(String key) {
        def gCloudAccount = gCloudAccountMap.get(key)
        if (!gCloudAccount) {
            throw new Exception("gCloudAccount entry '${key}' does not exist in config file")
        }
        return gCloudAccount
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
