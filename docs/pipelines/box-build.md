# BoxBuild

All properties and methods from [BoxCommon](box-common.md) are valid in addition to the properties and methods documented here.

## Methods

### composeBuild(profile)

Calls `docker-compose build` on the specified profile

### composeUp(profile)

Calls `docker-compose up` on the specified profile

### composeDown(profile)

Calls `docker-compose down` on the specified profile

### push()

Pushes images configured to matching registries, and writes event to build-versions repository

## Example

**jenkins.yaml:**

```yaml
common:
  images:
    - test/a
    - test/b
build:
  composeProfileMap:
    dev: ./docker/dev
    prod: ./docker/prod
  pullImages:
    - nginx
```

**Jenkinsfile:**

```groovy
@Library('jenkins-shared-library@master')
import com.boxboat.jenkins.pipeline.build.*

def build = new BoxBuild()

node() {
  build.wrap {
    stage('Test'){
      sh './build/ci/test.sh'
      sh './build/ci/test-api.sh'
    }
    stage('Build'){
      build.composeUp("prod")
    }
    stage ('Push'){
      build.push()
    }
  }
}
```
