package com.boxboat.jenkins.library.docker

class HarborImageManifest extends ImageManifest {

    HarborImageManifest(data) {
        super(data.name as String, data.digest as String, Date.parse('yyyy-MM-dd', data.push_time.substring(0, 10)))
    }

    static boolean isValid(data) {
        return (data != null) && (data.name != "") && (data.push_time != "") && (data.digest != "")
    }
}