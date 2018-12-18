# Deploy Config

## deploymentKey

Specify to deploy to a Deployment specified in `deploymentMap`.

```yaml
deploy:
  deploymentKey: dev
```

## deployTargetKey

Specify to deploy to a DeployTarget.

```yaml
deploy:
  deployTargetKey: dev01
```

## deploymentMap

- `environmentKey`: environment to deploy to
- `event`: primary event to pull image tags from
- `eventRegex`: regular expression matching events
- `eventFallback`: fallback event to pull image tags from
- `imageOverrides`: list of images to override for this deployment, see "imageOverrides" below for format
- `trigger`: if `true`, deployment will be automatically triggered when a matching image `event` occurs

```yaml
deploy:
  deploymentMap:
    dev:
      environmentKey: dev
      event: commit/master
      trigger: true
    feature:
      environmentKey: dev
      event: commit/feature-(.*)
      trigger: true
    stage:
      environmentKey: dev
      event: tag/rc
      imageOverrides:
      - path: test/b
        event: commit/develop
        eventFallback: commit/develop2
      trigger: true
    prod:
      environmentKey: prod
      event: tag/release
      trigger: false
```

## environmentKey

Specify to deploy to an environment.

```yaml
deploy:
  environmentKey: dev
```

## imageOverrides

List of images to override for every deployment

- `path`: image path
- `event`: primary event to pull image tags from
- `eventFallback`: fallback event to pull image tags from 

```yaml
deploy:
  imageOverrides:
    - path: test/b
      event: commit/develop
      eventFallback: commit/master
```
