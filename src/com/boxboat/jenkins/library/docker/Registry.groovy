package com.boxboat.jenkins.library.docker

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config

class Registry extends BaseConfig<Registry> implements Serializable {

    String scheme

    String host

    String namespace

    String credential

    String imageUrlReplace

    def getRegistryImageUrl(String path, String tag = "latest") {
        if (!imageUrlReplace || !path) {
            return ""
        }
        return imageUrlReplace.replaceFirst(/(?i)\{\{\s+path\s+\}\}/, path).replaceFirst(/(?i)\{\{\s+tag\s+\}\}/, tag)
    }

    def getRegistryUrl() {
        return "${scheme}://${host}"
    }

    def withCredentials(closure) {
        Config.pipeline.docker.withRegistry(getRegistryUrl(), credential, closure)
    }

}
