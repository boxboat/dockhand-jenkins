package com.boxboat.jenkins.library

import com.boxboat.jenkins.library.config.GlobalConfig

class LibraryScript {

    static String run(String script) {
        def path = "sharedLibraryScripts/${script}"
        if (!GlobalConfig.pipeline.fileExists(path)) {
            String data = GlobalConfig.pipeline.libraryResource(resource: "com/boxboat/jenkins/${path}", encoding: "Base64")
            GlobalConfig.pipeline.writeFile(file: path, text: data, encoding: "Base64")
            GlobalConfig.pipeline.sh "chmod +x ${path}"
        }
        return "./${path}"
    }
}
