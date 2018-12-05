package com.boxboat.jenkins.library.docker

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config

class Image extends BaseConfig<Image> {

    String event

    String eventFallback

    String host

    String path

    String tag

    Object trigger

    Image() {
    }

    Image(Map config) {
        config?.each { k, v -> this[k] = v }
    }

    Image(String imageString) {
        def hostPath = imageString.split("/", 2)
        if (hostPath.size() > 1 && hostPath[0].contains(".")) {
            host = hostPath[0]
            path = hostPath[1]
        } else {
            path = imageString
        }
        def pathTag = path.split(":", 2)
        if (pathTag.size() > 1) {
            path = pathTag[0]
            tag = pathTag[1]
        }
    }

    String getUrl() {
        return (host ? "${host}/" : "") + "${path}:${tag ?: "latest"}"
    }

    Image copy() {
        return new Image(host: host, path: path, tag: tag)
    }

    Image reTag(Image newImage) {
        Config.pipeline.sh """
            docker tag "${this.url}" "${newImage.url}"
        """
        return newImage
    }

    void push() {
        Config.pipeline.sh """
            docker push "${this.url}"
        """
    }

    void pull() {
        Config.pipeline.sh """
            docker pull "${this.url}"
        """
    }

}
