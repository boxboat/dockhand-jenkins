package com.boxboat.jenkins.library.config

import com.boxboat.jenkins.library.aws.AwsProfile
import com.boxboat.jenkins.library.deployTarget.IDeployTarget
import com.boxboat.jenkins.library.docker.Registry
import com.boxboat.jenkins.library.environment.Environment
import com.boxboat.jenkins.library.gcloud.GCloudAccount
import com.boxboat.jenkins.library.git.GitConfig
import com.boxboat.jenkins.library.notify.INotifyTarget
import com.boxboat.jenkins.library.vault.Vault

class GlobalConfig extends BaseConfig<GlobalConfig> implements Serializable {

    Map<String, AwsProfile> awsProfileMap

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

    String getGitRemotePath(String key, String url) {
        def gitConfig = git.getGitConfig(key)
        if (!gitConfig.remotePathRegex) {
            return ""
        }
        def prefix = key ? "${key}:" : ""
        def matcher = url =~ gitConfig.remotePathRegex
        return matcher.hasGroup() && matcher.size() > 0 ? "${prefix}${matcher[0][1]}" : null
    }

    String getGitRemoteUrl(String path) {
        def matcher = path =~ /(.*):.*/
        def key = matcher.hasGroup() && matcher.size() > 0 ? matcher[0][1] : null
        def gitConfig = git.getGitConfig(key as String)
        if (!gitConfig.remoteUrlReplace || !path) {
            return ""
        }
        if (key) {
            path = path.substring((key as String).length() + 1)
        }
        return GitConfig.replacePath(gitConfig.remoteUrlReplace, path)
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
