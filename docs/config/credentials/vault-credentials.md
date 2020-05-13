# Vault Credentials
[Hashicorp Vault](https://www.vaultproject.io/) may be used as a credential store for Dockhand. To do so a `vaultMap` must be defined as described in the [Global Config](../global.md). For credentials key in the config map, a `vault` from the `vaultMap`, `path` at that vault location and key(s) at that path may be specified. To do so, the credential key should be defined with a reference to the vault credential type we are reading (ie `credential: !!com.boxboat.jenkins.library.credentials.vault.<credential-type>`, where `credential-type` may be: `VaultFileCredential`, `VaultStringCredential`, or `VaultUsernamePasswordCredential`). Different configuration blocks require different types of vault credentials, which are details below.

Note that to make use of the Vault credentials, [dockcmd](https://github.com/boxboat/dockcmd) must be installed on the jenkins agents. This functionality also requires the mask-passwords plugin for Jenkins to be installed.

## git
```yaml
git:
  # repository where build versions are written to
  buildVersionsUrl: git@github.com:boxboat/build-versions.git
  # SSH key credential for git service account
  credential: !!com.boxboat.jenkins.library.credentials.vault.VaultFileCredential
      # The vault in the vaultMap to read this credential from
      vault: default
      # The path in vault where this credential is stored
      path: kv/git
      # The key at the path that contains the kubernetes config contents
      fileKey: sshKey
```

## deployTargetMap
```yaml
deployTargetMap:
  dev02: !!com.boxboat.jenkins.library.deployTarget.KubernetesDeployTarget
    # kubernetes context to use
    contextName: boxboat
    # Vault credential info (expects a kubeconfig file at the location specified)
    credential: !!com.boxboat.jenkins.library.credentials.vault.VaultFileCredential
      # The vault in the vaultMap to read this credential from
      vault: default
      # The path in vault where this credential is stored
      path: kv/kubeconfig
      # The key at the path that contains the kubernetes config contents
      fileKey: prod03
```

## nofityTargetMap
```yaml
notifyTargetMap:
  prod: !!com.boxboat.jenkins.library.notify.SlackWebHookNotifyTarget
     # Vault credential info (expects a slack webhook url at the location specified)
    credential: !!com.boxboat.jenkins.library.credentials.vault.VaultStringCredential
      # The vault in the vaultMap to read this credential from
      vault: default
      # The path in vault where this credential is stored
      path: kv/slack-prod-credentials
      # The key at the path that contains the slack webhook url
      stringKey: webhook-url
```

## registryMap
```yaml
registryMap:
  dev:
    scheme: https
    host: dtr.boxboat.com
    # Vault credential info (expects username and password keys at the location specified)
    credential: !!com.boxboat.jenkins.library.credentials.vault.VaultUsernamePasswordCredential
      # The vault in the vaultMap to read this credential from
      vault: default
      # The path in vault where this credential is stored
      path: kv/harbor-dev-credentials
      # The key at the path in vault that stores the username for this registry
      usernameKey: username
      # The key at the path in vault that stores the password for this registry
      passwordKey: password
    imageUrlReplace: https://dtr.boxboat.com/repositories/{{ path }}/{{ tag }}/linux/amd64/layers
```