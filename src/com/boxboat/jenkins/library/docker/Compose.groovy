package com.boxboat.jenkins.library.docker

import com.boxboat.jenkins.library.LibraryScript

class Compose {

    static String up(steps, dir, profile) {
        steps.sh """
            ${LibraryScript.run(steps, "compose-up.sh")} "$dir" "$profile"
        """
    }

    static String down(steps, dir, profile) {
        steps.sh """
            ${LibraryScript.run(steps, "compose-down.sh")} "$dir" "$profile"
        """
    }

    static String build(steps, dir, profile) {
        steps.sh """
            ${LibraryScript.run(steps, "compose-build.sh")} "$dir" "$profile"
        """
    }

}
