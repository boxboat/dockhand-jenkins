package com.boxboat.jenkins.library

class LibraryScript {

    static Map<String, Boolean> loaded = [:]

    static String run(steps, String script) {
        def path = "sharedLibraryScripts/${script}"
        if (!loaded.containsKey(script)) {
            String data = steps.libraryResource(resource: "com/boxboat/jenkins/${path}", encoding: "Base64")
            steps.writeFile(file: path, text: data, encoding: "Base64")
            steps.sh "chmod +x ${path}"
            loaded[script] = true
        }
        return "./${path}"
    }
}
