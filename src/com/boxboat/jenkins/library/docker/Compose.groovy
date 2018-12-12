package com.boxboat.jenkins.library.docker

import com.boxboat.jenkins.library.LibraryScript
import com.boxboat.jenkins.library.config.Config

class Compose implements Serializable {

    static String up(dir, profile) {
        Config.pipeline.sh """
            ${LibraryScript.run("compose-up.sh")} "$dir" "$profile"
        """
    }

    static String down(dir, profile) {
        Config.pipeline.sh """
            ${LibraryScript.run("compose-down.sh")} "$dir" "$profile"
        """
    }

    static String build(dir, profile) {
        Config.pipeline.sh """
            ${LibraryScript.run("compose-build.sh")} "$dir" "$profile"
        """
    }

}
