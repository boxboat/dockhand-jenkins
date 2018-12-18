# Build Config

## composeProfileMap

Key is profile name, value is directory where `docker-compose.yaml` file is located

```yaml
build:
  composeProfileMap:
    dev: ./docker/dev
    prod: ./docker/prod
```

## pullImages

List of images to pull

```yaml
build:
  pullImages:
    - dtr.boxboat.com/test/a
    - dtr.boxboat.com/test/b
```
