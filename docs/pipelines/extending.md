# Extending

The shared library should be extended using an organization's own package.

- `com.<org>.jenkins.library` for internal shared library logic
- `com.<org>.jenkins.pipeline` for classes called from pipelines

## Configuration

Configuration can also be extended for use with user-defined library or pipeline functions. The [common config](../config/common) page describes the keys to use for this. Below is an example class that would be used to store that data. The data could then be retreived by calling `config.getUserConfig('example')`

```groovy
package com.example.jenkins.library.config

import com.boxboat.jenkins.library.config.BaseConfig

class ArbitraryConfig extends BaseConfig<ArbitraryConfig> implements Serializable {

    String foo

    String bar
}
```