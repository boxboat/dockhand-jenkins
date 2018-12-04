package com.boxboat.jenkins.library.git

import com.boxboat.jenkins.library.config.GlobalConfig

import java.nio.file.Paths

class GitRepo implements Serializable {

    public checkoutData = [:]
    public relativeDir

    private String _dir

    private String getDir() {
        if (!_dir) {
            _dir = Paths.get(GlobalConfig.pipeline.env.WORKSPACE, relativeDir).toAbsolutePath().toString()
        }
        return _dir
    }

    String _hash

    String getHash() {
        if (!_hash) {
            _hash = checkoutData?.GIT_COMMIT ?: GlobalConfig.pipeline.sh(returnStdout: true, script: """
                cd "${this.dir}"
                git show -s --format=%H
            """)
        }
        return _hash
    }

    String getShortHash() {
        return getHash()?.substring(0, 12)
    }

    String _branch

    String getBranch() {
        if (!_branch) {
            _branch = checkoutData?.GIT_BRANCH ?: GlobalConfig.pipeline.sh(returnStdout: true, script: """
                git rev-parse --abbrev-ref HEAD
            """)
        }
        return _branch
    }

    String getRemoteUrl() {
        return GlobalConfig.pipeline.sh(returnStdout: true, script: """
            cd "${this.dir}"
            git config remote.origin.url
        """)
    }

    String getRemotePath() {
        return GlobalConfig.config.gitRemotePath(getRemoteUrl())
    }

    boolean isBranchTip() {
        String originHash = GlobalConfig.pipeline.sh(returnStdout: true, script: """
            git show-branch --sha1-name origin/${this.branch} || :
        """)
        def matcher = originHash =~ /^\[([0-9a-f]+)\]/
        String tipHash = matcher.hasGroup() ? matcher[0][1] : null
        if (!tipHash) {
            return false
        }
        return hash.startsWith(tipHash)
    }

    def checkoutBranch(branch) {
        GlobalConfig.pipeline.sh """
            cd "${this.dir}"
            if git rev-parse --verify "${branch}" > /dev/null 2>&1
            then
                git checkout --orphan "jenkins-branch-reset-orphan"
                git branch -D "${branch}"
                git reset --hard
            fi
            git checkout "${branch}"
            git clean -fd
        """
    }

    def resetToHash(commitHash, branch) {
        this.checkoutBranch(branch)
        GlobalConfig.pipeline.sh """
            cd "${this.dir}"
            git reset --hard "${commitHash}"
            git clean -fd
        """
    }

    def resetAndClean() {
        GlobalConfig.pipeline.sh """
            cd "${this.dir}"
            git reset --hard
            git clean -fd
        """
    }

    def shallowCheckoutBranch(branch) {
        GlobalConfig.pipeline.sh """
            cd "${this.dir}"
            git remote set-branches origin "${branch}"
            git fetch --depth 1 origin "${branch}"
            git checkout "${branch}"
        """
    }

    def commitAndPush(commitMessage) {
        GlobalConfig.pipeline.sh(returnStdout: true, script: """
            cd "${this.dir}"
            git add --all
            
            # Only commit/push if there are changes
            git update-index -q --refresh
            if git diff-index --quiet HEAD -- ; then
              # No changes
              echo "Build-versions file not changed, skipping"
            else
              # Changes
              git commit -a -m "${commitMessage}"
              git push
            fi
        """)
    }

    def forcePush() {
        GlobalConfig.pipeline.sh """
            cd "${this.dir}"
            git push -f
        """
    }

    def rollback() {
        GlobalConfig.pipeline.sh """
            cd "${this.dir}"
            git reset --hard HEAD~1
        """
    }

    def updateSubmodules() {
        GlobalConfig.pipeline.sh """
            cd "${this.dir}"
            git submodule update --init --recursive
        """
    }

}
