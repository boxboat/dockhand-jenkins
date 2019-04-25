# BoxCommon

Base class for pipelines

## Properties

### dir

Directory to change into after checking out from SCM

## Methods

### wrap()

Wraps pipeline code in block with access to all helper methods in the shared library.

Sends success notifications upon build success and failure notifications upon build failure.

## Example

**Jenkinsfile:**

```groovy
@Library('jenkins-shared-library@master')
import com.boxboat.jenkins.pipeline.common.*

def common = new BoxCommon(
  dir: "./test"
)

node() {
  common.wrap {
    stage('Test'){
      // access to helper methods here
    }
  }
}
```
