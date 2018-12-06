package com.boxboat.jenkins.library

import com.boxboat.jenkins.library.config.Config

class LibraryScript {

    static String run(String script) {
        def path = "sharedLibraryScripts/${script}"
        if (Config.pipeline && !Config.pipeline.fileExists(path)) {
            String data = Config.pipeline.libraryResource(resource: "com/boxboat/jenkins/${path}", encoding: "Base64")
            Config.pipeline.writeFile(file: path, text: data, encoding: "Base64")
            Config.pipeline.sh "chmod +x ${path}"
        }
        return "./${path}"
    }
}
