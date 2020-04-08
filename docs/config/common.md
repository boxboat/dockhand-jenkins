# Common Config

## defaultBranch

Default branch.  Triggers will be run against this branch.

```yaml
common:
  defaultBranch: master
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

## vaultKey

Vault key to use

```yaml
common:
  vaultKey: default
```
