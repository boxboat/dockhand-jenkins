# Common Config

## defaultBranch

Default branch.  Triggers will be run against this branch.

```yaml
common:
  defaultBranch: master
```

## prUseTargetBranch

If your git solution supports PR Branches, then you can set this to `true` if you would like to build PRs but retain the actual git branch name.
This will ensure that build and deployment logic based on branch name events function correctly within Dockhand.
If you are using `commit/PR-*` events then leave this setting set to `false`
```yaml
  prUseTargetBranch: false
```

## eventRegistryKeys

Events that match `event` or `eventRegex` will be pushed to to the registry at `registryKey`.

```yaml
common:
  eventRegistryKeys:
    - event: commit/master
      registryKey: default
    - eventRegex: tag/(.*)
      registryKey: default
```

## gitAlternateKey

Alternate Git configuration to select from Global Config [git.gitAlternateMap](global.md#git)

```yaml
common:
  gitAlternateKey: gitlab
```

## images

`images` are used in the following way:

- Build Pipeline: will be pushed to matching registries
- Promote Pipeline: will be promoted
- Deploy Pipeline: image tags will be computed

```yaml
common:
  images:
    - test/a
    - test/b
```

## notify

Notification settings.  Keys are references to targets defined in `targetMap` or the global `notifyTargetMap`.
Supported notification target types are:

- `com.boxboat.jenkins.library.notify.SlackWebHookNotifyTarget`
  - For use with the [Slack Incoming Webhooks App](https://boxboat.slack.com/apps/A0F7XDUAZ-incoming-webhooks?next_id=0)
  - Jenkins Credential referenced in `credential` is a Secret Text credential with the full webhook URL
- `com.boxboat.jenkins.library.notify.SlackJenkinsAppNotifyTarget`
  - For use with the [Slack Jenkns CI App](https://boxboat.slack.com/apps/A0F7VRFKN-jenkins-ci?next_id=0)
  - Use `channel` to override channel

```yaml
common:
  notify:
    targetMap:
      jenkins: !!com.boxboat.jenkins.library.notify.SlackJenkinsAppNotifyTarget
        channel: "#jenkins"
    successKeys:
      - default
      - jenkins
    successTargets:
      - !!com.boxboat.jenkins.library.notify.SlackJenkinsAppNotifyTarget
        channel: "#jenkins-success"
    failureKeys:
      - default
      - jenkins
    failureTargets:
      - !!com.boxboat.jenkins.library.notify.SlackJenkinsAppNotifyTarget
        channel: "#jenkins-failure"
    infoKeys:
      - default
      - jenkins
    infoTargets:
      - !!com.boxboat.jenkins.library.notify.SlackJenkinsAppNotifyTarget
        channel: "#jenkins-info"
```

## userConfigMap
Users can define their own arbitrary configuration for use with their own library functions. The below example requires that a class called `ArbitraryConfig` exists which can accept `foo` and `bar`. See the [extending documentation](../pipeline/extending.md) for a sample class.

```yaml
common:
  userConfigMap:
    example: !!com.example.jenkins.library.config.ArbitraryConfig
      foo: biz
      bar: baz
```

## vaultKey

Vault key to use

```yaml
common:
  vaultKey: default
```
