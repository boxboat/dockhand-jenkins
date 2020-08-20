# BoxPromote

All properties and methods from [BoxCommon](box-common.md) are valid in addition to the properties and methods documented here.

## Methods

### promote()

Promotes images and pushes git tag to repository

## Example

**jenkins.yaml:**

```yaml
common:
  images:
    - test/a
    - test/b
promote:
  baseVersion: "0.1.0"
```

**Jenkinsfile:**

```groovy
@Library('jenkins-shared-library@master')
import com.boxboat.jenkins.pipeline.promote.*

def promotions = ["", "stage", "prod"]
properties([
  parameters([
    choice(name: 'promotionKey', choices: promotions, description: 'Promotion', defaultValue: '')

    //Add these if you want to manually create a promoted version. Note if 'overridePromoteToEvent' is set, the image tag will not be written to build-versions
    string(name: 'overrideEvent', description: 'Override promote from event, typically commit/<branch>, tag/<tag>, or imageTag/<imageTag>', defaultValue: '')
    string(name: 'overridePromoteToEvent', description: 'Override promote to event, tag/<tag>, or imageTag/<imageTag>', defaultValue: '')
  ])
])

def promote = new BoxPromote()

node() {
  promote.wrap {
    stage('Promote'){
      promote.promote()
    }
  }
}
```
