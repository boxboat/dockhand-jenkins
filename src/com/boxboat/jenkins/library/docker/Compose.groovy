package com.boxboat.jenkins.library.docker

import com.boxboat.jenkins.library.LibraryScript
import com.boxboat.jenkins.library.config.GlobalConfig

class Compose {

    static String up(dir, profile) {
        GlobalConfig.pipeline.sh """
            ${LibraryScript.run("compose-up.sh")} "$dir" "$profile"
        """
    }

    static String down(dir, profile) {
        GlobalConfig.pipeline.sh """
            ${LibraryScript.run("compose-down.sh")} "$dir" "$profile"
        """
    }

    static String build(dir, profile) {
        GlobalConfig.pipeline.sh """
            ${LibraryScript.run("compose-build.sh")} "$dir" "$profile"
        """
    }

}
