package com.boxboat.jenkins.library

import com.boxboat.jenkins.library.config.Config

import java.nio.file.Paths

class LibraryScript implements Serializable {

    static String run(String script) {
        def relativePath = Paths.get("sharedLibraryScripts", script).toString()
        def absolutePath = Paths.get(Config.baseDir, relativePath).normalize().toAbsolutePath().toString()
        if (Config.pipeline && !Config.pipeline.fileExists(absolutePath)) {
            String data = Config.pipeline.libraryResource(resource: "com/boxboat/jenkins/${relativePath}", encoding: "Base64")
            Config.pipeline.writeFile(file: absolutePath, text: data, encoding: "Base64")
            Config.pipeline.sh "chmod +x ${absolutePath}"
        }
        return absolutePath
    }
}
