package com.boxboat.jenkins.library.docker

class Compose {

    static String up(dir, profile, registry) {
        return """
            ./sharedLibraryScripts/compose-up.sh "$dir" "$profile" "$registry"
        """
    }

    static String down(dir, profile) {
        return """
            ./sharedLibraryScripts/compose-down.sh "$dir" "$profile"
        """
    }

    static String build(dir, profile, registry) {
        return """
            ./sharedLibraryScripts/compose-build.sh "$dir" "$profile" "$registry"
        """
    }

}
