package com.boxboat.jenkins.library.docker

class ArtifactoryImageManifest extends ImageManifest {

    ArtifactoryImageManifest(data) {
        super(data.name as String, data.digest as String, Date.parse('yyyy-MM-dd', data.updatedAt.substring(0, 10)))
    }

    static boolean isValid(data) {
        return (data != null) && (data.name != "") && (data.updatedAt != "") && (data.digest != "")
    }
}
