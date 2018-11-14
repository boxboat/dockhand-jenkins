package com.boxboat.jenkins.library.git

import static com.boxboat.jenkins.library.Config.Config

class GitAccount implements Serializable {

    public steps
    private _initialized = false

    private _ensureInitialized() {
        if (_initialized) {
            return true
        }
        steps.withCredentials([steps.sshUserPrivateKey(
                credentialsId: Config.gitCredentials,
                keyFileVariable: 'sshKey',
                usernameVariable: 'username'
        )]) {
            steps.sh """
                # Private Key
                mkdir -p ~/.ssh
                chmod 400 "${steps.env.sshKey}"
                mv "${steps.env.sshKey}" ~/.ssh/id_rsa

                # Git Config
                git config --global user.name "Jenkins Service Account"
                git config --global user.email "${Config.gitEmail}"
                git config --global push.default simple
            """
        }
        _initialized = true
    }

    // Checkout SCM
    def checkoutScm() {
        _ensureInitialized()
        steps.sh """
            if [ -d .git ]
            then
                git add --all
                git stash || :
                git stash drop || :
                git clean -fd
            fi
        """
        def checkoutData = steps.checkout steps.scm
        return new GitRepo(steps: steps, relativeDir: ".", checkoutData: checkoutData)
    }

    // Checkout Remote Repository into a targetDir
    def checkoutRepository(remoteUrl, targetDir, depth = 0) {
        _ensureInitialized()
        def depthStr = ""
        if (depth > 0) {
            depthStr = "--depth ${depth}"
        }
        steps.sh """
            rm -rf "${targetDir}"
            mkdir -p "${targetDir}"
            cd "${targetDir}"
            git clone ${depthStr} "${remoteUrl}" .
        """

        return new GitRepo(steps: steps, relativeDir: targetDir)
    }

}
