package com.boxboat.jenkins.library.docker

class Image {

    String host = ""

    String path

    String tag = "latest"

    String getUrl() {
        return (host ? "${host}/" : "") + "${path}:${tag}"
    }

    Image copy() {
        return new Image(host: host, path: path, tag: tag)
    }

    Image reTag(steps, Image newImage) {
        steps.sh """
            docker tag "${this.url}" "${newImage.url}"
        """
        return newImage
    }

    void push(steps) {
        steps.sh """
            docker push "${this.url}"
        """
    }

    void pull(steps) {
        steps.sh """
            docker pull "${this.url}"
        """
    }

    static Image fromImageString(String imageString) {
        def image = new Image()
        def hostPath = imageString.split("/", 2)
        if (hostPath.size() > 1 && hostPath[0].contains(".")) {
            image.host = hostPath[0]
            image.path = hostPath[1]
        } else {
            image.path = imageString
        }
        def pathTag = image.path.split(":", 2)
        if (pathTag.size() > 1) {
            image.path = pathTag[0]
            image.tag = pathTag[1]
        }
        return image
    }

}
