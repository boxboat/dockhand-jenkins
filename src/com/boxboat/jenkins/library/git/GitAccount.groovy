package com.boxboat.jenkins.library.git

import com.boxboat.jenkins.library.config.GlobalConfig

class GitAccount implements Serializable {

    private _initialized = false

    private _ensureInitialized() {
        if (_initialized) {
            return true
        }
        GlobalConfig.pipeline.withCredentials([GlobalConfig.pipeline.sshUserPrivateKey(
                credentialsId: GlobalConfig.config.git.credential,
                keyFileVariable: 'sshKey',
                usernameVariable: 'username'
        )]) {
            GlobalConfig.pipeline.sh """
                # Private Key
                mkdir -p ~/.ssh
                chmod 400 "${GlobalConfig.pipeline.env.sshKey}"
                mv "${GlobalConfig.pipeline.env.sshKey}" ~/.ssh/id_rsa

                # Git GlobalConfig
                git config --global user.name "Jenkins Service Account"
                git config --global user.email "${GlobalConfig.config.git.email}"
                git config --global push.default simple
            """
        }
        _initialized = true
    }

    // Checkout SCM
    def checkoutScm() {
        _ensureInitialized()
        GlobalConfig.pipeline.sh """
            set +ex
            if [ -d .git ]
            then
                git reset --hard
                git clean -fd
            fi
            exit 0
        """
        def checkoutData = GlobalConfig.pipeline.checkout GlobalConfig.pipeline.scm
        return new GitRepo(relativeDir: ".", checkoutData: checkoutData)
    }

    // Checkout Remote Repository into a targetDir
    def checkoutRepository(remoteUrl, targetDir, depth = 0) {
        _ensureInitialized()
        def depthStr = ""
        if (depth > 0) {
            depthStr = "--depth ${depth}"
        }
        GlobalConfig.pipeline.sh """
            rm -rf "${targetDir}"
            mkdir -p "${targetDir}"
            cd "${targetDir}"
            git clone ${depthStr} "${remoteUrl}" .
        """

        return new GitRepo(relativeDir: targetDir)
    }

}
