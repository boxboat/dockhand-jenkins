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
    choice(name: 'promotionKey', choices: deployments, description: 'Promotion', defaultValue: '')
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
