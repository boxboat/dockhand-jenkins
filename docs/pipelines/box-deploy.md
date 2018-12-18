# BoxBuild

## Methods

### writeImageTags(Map)

Writes image tags from build-versions to specified file

**Parameters**:
- `format`: either `env` or `yaml`.  If not specified, will attempt to detect from `outFile` extension
- `outFile`: file path to write
- `yamlPath`: list of parent keys to prepend to generated yaml

### withCredentials()

Adds the deployTarget credentials

## Example

**jenkins.yaml:**

```yaml
common:
  images:
    - test/a
    - test/b
deploy:
  imageOverrides:
    - path: test/b
      event: commit/develop
      eventFallback: commit/master
```

**Jenkinsfile:**

```groovy
@Library('jenkins-shared-library@master')
import com.boxboat.jenkins.pipeline.deploy.*

def deployments = ["", "dev", "stage", "prod"]
properties([
  parameters([
    choice(name: 'deploymentKey', choices: deployments, description: 'Deployment', defaultValue: '')
  ])
])

def build = new BoxDeploy()

node() {
  deploy.wrap {
    stage('Deploy'){
      deploy.writeImageTags(
        outFile: "image-tags.yaml",
        yamlPath: ["global"],
      )
      deploy.withCredentials() {
        sh "helm upgrade --install test ."
      }
    }
  }
}
```
