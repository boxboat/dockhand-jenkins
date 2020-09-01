# BoxPromote

All properties and methods from [BoxCommon](box-common.md) are valid in addition to the properties and methods documented here.

## Methods

### promote()

Promotes images and pushes git tag to repository

Notes on using the `promoteToVersion` override. This is intended for special occasions where the release needs to be manipulated or an older release requires a patch bump. This key can also be used to promote to a major or minor version on a `tag/release` event without the need to modify the `baseVersion` in `jenkins.yaml`. Some notes on the functionality
- If you use promoteToVersion to override the registries defined for your `promotionKey` will be used to store any promoted images
- If git tags are used the `promoteToVersion` will be tagged on the promoted commit
- Promoting to a version that is greater than the current version will write it back to build-versions as long as the version type matches a version that would normally be written using that `promotionKey` (ie. writing an rc tag on a `tag/release` event will not write back to build-versions or trigger a deployment).
- As a general rule, do not use promoteToVersion for pre-release versions. If you are releasing a pre-release version with `promoteToVersion` and there is a currently released pre-release with the same `major.minor.patch` semantic version (ie. `promoteToVersion = 1.0.1-rc5` and `currentSemVer = 1.0.1-rc100`), the `promoteToVersion` version will overwrite the `currentSemVer` in build versions

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
    string(name: 'promoteToVersion', description: 'Set a semver to promote to. Overrides patch bumping behavior', defaultValue: '')
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
