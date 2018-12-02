package com.boxboat.jenkins.library.config

import com.boxboat.jenkins.library.docker.Image

class BuildConfig extends CommonConfigBase<BuildConfig> {

    Map<String, String> composeProfileMap

    List<Image> pullImages

}
