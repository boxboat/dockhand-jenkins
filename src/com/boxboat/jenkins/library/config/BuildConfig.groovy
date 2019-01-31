package com.boxboat.jenkins.library.config

import com.boxboat.jenkins.library.docker.Image

class BuildConfig extends CommonConfigBase implements Serializable {

    Map<String, String> composeProfileMap

    List<Image> pullImages

}
