package com.boxboat.jenkins.library.git

import static com.boxboat.jenkins.library.Config.Config

import java.nio.file.Paths

class GitRepo implements Serializable {

    public checkoutData = [:]
    public steps
    public relativeDir

    private String _dir

    private String getDir() {
        if (!_dir) {
            _dir = Paths.get(steps.env.WORKSPACE, relativeDir).toAbsolutePath().toString()
        }
        return _dir
    }

    String _hash

    String getHash() {
        if (!_hash) {
            _hash = checkoutData?.GIT_COMMIT ?: steps.sh(returnStdout: true, script: """
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
            _branch = checkoutData?.GIT_BRANCH ?: steps.sh(returnStdout: true, script: """
                git rev-parse --abbrev-ref HEAD
            """)
        }
        return _branch
    }

    String getRemoteUrl() {
        return steps.sh(returnStdout: true, script: """
            cd "${this.dir}"
            git config remote.origin.url
        """)
    }

    String getRemotePath() {
        return Config.gitRemotePath(getRemoteUrl())
    }

    boolean isBranchTip() {
        String originHash = steps.sh(returnStdout: true, script: """
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
        steps.sh """
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
        steps.sh """
            cd "${this.dir}"
            git reset --hard "${commitHash}"
            git clean -fd
        """
    }

    def resetAndClean() {
        steps.sh """
            cd "${this.dir}"
            git reset --hard
            git clean -fd
        """
    }

    def shallowCheckoutBranch(branch) {
        steps.sh """
            cd "${this.dir}"
            git remote set-branches origin "${branch}"
            git fetch --depth 1 origin "${branch}"
            git checkout "${branch}"
        """
    }

    def commitAndPush(commitMessage) {
        steps.sh(returnStdout: true, script: """
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
        steps.sh """
            cd "${this.dir}"
            git push -f
        """
    }

    def rollback() {
        steps.sh """
            cd "${this.dir}"
            git reset --hard HEAD~1
        """
    }

    def updateSubmodules() {
        steps.sh """
            cd "${this.dir}"
            git submodule update --init --recursive
        """
    }

}
