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

## notifySuccessKeys

Notify targets to send success notifications to

```yaml
common:
  notifySuccessKeys:
    - slack-success
```

## notifyFailureKeys

Notify targets to send failure notifications to

```yaml
common:
  notifyFailureKeys:
    - slack-failure
```

## vaultKey

Vault key to use

```yaml
common:
  vaultKey: default
```
