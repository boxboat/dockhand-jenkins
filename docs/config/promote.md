# Promote Config

All keys from the [Common Config](common.md) are valid in addition to the keys documented here.

## baseVersion

Base version to use when tagging promotions.  Patch versions will be automatically increased

```yaml
promote:
  baseVersion: "0.1.0"
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
