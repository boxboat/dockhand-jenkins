package com.boxboat.jenkins.library.git

import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.Config

class GitAccount implements Serializable {

    private _initialized = false

    private _ensureInitialized() {
        if (_initialized) {
            return true
        }
        Config.global.git.withCredentials([
                'keyFileVariable' : 'sshKey',
                'usernameVariable': 'username']) {
            Config.pipeline.sh """
                # Private Key
                mkdir -p ~/.ssh
                chmod 400 "${Config.pipeline.env.sshKey}"
                mv "${Config.pipeline.env.sshKey}" ~/.ssh/id_rsa

                # Git GlobalConfig
                git config --global user.name "Jenkins Service Account"
                git config --global user.email "${Config.global.git.email}"
                git config --global push.default simple
            """
        }
        _initialized = true
    }

    // Checkout SCM
    def checkoutScm() {
        _ensureInitialized()
        Config.pipeline.sh """
            set +ex
            if [ -d .git ]
            then
                git reset --hard
                git clean -fd
            fi
            exit 0
        """
        def checkoutData = Config.pipeline.checkout Config.pipeline.scm
        def repo = new GitRepo(dir: Utils.toAbsolutePath("."))

        repo.setBranch(checkoutData?.GIT_BRANCH)

        if (Config.pipeline.env?.CHANGE_BRANCH){
            if (Config.repo.prUseTargetBranch) {
                repo.setBranch(Config.pipeline.env.CHANGE_BRANCH)
            }
            repo.setPrBranch(checkoutData?.GIT_BRANCH)
        }

        return repo
    }

    // Checkout Remote Repository into a targetDir
    def checkoutRepository(String remoteUrl, String targetDir, int depth = 0) {
        _ensureInitialized()
        def depthStr = ""
        if (depth > 0) {
            depthStr = "--depth ${depth}"
        }
        Config.pipeline.sh """
            rm -rf "${targetDir}"
            mkdir -p "${targetDir}"
            cd "${targetDir}"
            if [ -z "\$GIT_SSH_COMMAND" ]; then
                export GIT_SSH_COMMAND="ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no"
            fi
            git clone ${depthStr} "${remoteUrl}" .
        """

        return new GitRepo(dir: Utils.toAbsolutePath(targetDir))
    }

}
