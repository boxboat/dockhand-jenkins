package com.boxboat.jenkins.library.docker

class DtrImageManifest  extends ImageManifest {

    DtrImageManifest(data) {
        super(data.name as String, data.digest as String, Date.parse('yyyy-MM-dd', data.updatedAt.substring(0, 10)))
    }

    static boolean isValid(data) {
        return (data != null) && (data.name != "") && (data.updatedAt != "") && (data.digest != "")
    }
}
