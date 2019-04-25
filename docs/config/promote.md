# Promote Config

All keys from the [Common Config](common.md) are valid in addition to the keys documented here.

## baseVersion

Base version to use when tagging promotions.  Patch versions will be automatically increased

```yaml
promote:
  baseVersion: "0.1.0"
```

## gitTagDisable

Default is `false`.  Set to `true` to disable adding a Git tag to the repository

```yaml
promote:
  gitTagDisable: true
```

## gitTagPrefix

Set to add a prefix to the Git tag that will be applied to the repository

```yaml
promote:
  # example: SemVer is 1.0.0
  # git tag will be "base-images/nginx/1.0.0"
  gitTagPrefix: "base-images/nginx/"
```

## promotionKey

Key of the promotion from `promotionMap` to run

```yaml
promote:
  promotionKey: "stage"
```

## promotionMap

Map of promotion options.  The latest image pushed to `event` will be promoted to `promoteToEvent`

```yaml
promote:
  promotionMap:
    stage:
      event: commit/master
      promoteToEvent: tag/rc
    prod:
      event: tag/rc
      promoteToEvent: tag/release
```
