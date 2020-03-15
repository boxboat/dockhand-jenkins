# Build Config

## composeProfileMap

Key is profile name, value is directory where `docker-compose.yaml` file is located or full path to docker-compose yaml file

```yaml
build:
  composeProfileMap:
    dev: ./docker/
    prod: ./docker/docker-compose-prod.yaml
```

## pullImages

List of images to pull

```yaml
build:
  pullImages:
    - dtr.boxboat.com/test/a
    - dtr.boxboat.com/test/b
```
