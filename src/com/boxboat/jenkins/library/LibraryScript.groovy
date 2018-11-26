package com.boxboat.jenkins.library

class LibraryScript {

    static String run(steps, String script) {
        def path = "sharedLibraryScripts/${script}"
        if (!steps.fileExists(path)) {
            String data = steps.libraryResource(resource: "com/boxboat/jenkins/${path}", encoding: "Base64")
            steps.writeFile(file: path, text: data, encoding: "Base64")
            steps.sh "chmod +x ${path}"
        }
        return "./${path}"
    }
}
