package com.boxboat.jenkins.library.git

import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.Config

import java.nio.file.Paths

class GitRepo implements Serializable {

    public checkoutData = [:]
    public relativeDir

    protected String dir

    public String getDir() {
        if (!dir) {
            dir = Paths.get(Config.pipeline.env.WORKSPACE, relativeDir).toAbsolutePath().toString()
        }
        return dir
    }

    String getHash() {
        return Config.pipeline.sh(returnStdout: true, script: """
            cd "${this.dir}"
            git show -s --format=%H
        """)?.trim()
    }

    String getShortHash() {
        return getHash()?.substring(0, 12)
    }

    String _branch

    String getBranch() {
        if (!_branch) {
            _branch = checkoutData?.GIT_BRANCH ?: Config.pipeline.sh(returnStdout: true, script: """
                git rev-parse --abbrev-ref HEAD
            """)?.trim()
        }
        return _branch
    }

    String setBranch(String value) {
        _branch = value
    }

    String getRemoteUrl() {
        def result = Config.pipeline.sh(returnStdout: true, script: """
            cd "${this.dir}"
            git config remote.origin.url
        """)?.trim()
        return Utils.resultOrTest(result, Config.global.git.buildVersionsUrl)
    }

    String getRemotePath() {
        return Config.global.git.getRemotePath(getRemoteUrl())
    }

    boolean isBranchTip() {
        String originHash = Config.pipeline.sh(returnStdout: true, script: """
            git show-branch --sha1-name origin/${this.getBranch()} || :
        """)
        def matcher = originHash =~ /^\[([0-9a-f]+)\]/
        String tipHash = matcher.hasGroup() ? matcher[0][1] : null
        if (!tipHash) {
            return false
        }
        return hash.startsWith(tipHash)
    }

    def checkout(String checkout) {
        Config.pipeline.sh """
            cd "${this.dir}"
            git checkout "${checkout}"
            git reset --hard
            git clean -fd
        """
    }

    boolean currentBranchContainsCommit(String commit) {
        def containsCommit = Config.pipeline.sh(returnStdout: true, script: """
            git branch ${this.getBranch()} --contains "${commit}"
        """)?.trim()
        return containsCommit ? true : false
    }

    def resetToHash(String commitHash) {
        Config.pipeline.sh """
            cd "${this.dir}"
            git reset --hard "${commitHash}"
            git clean -fd
        """
    }

    def tagAndPush(String tag) {
        Config.pipeline.sh """
            cd "${this.dir}"
            git tag -f "${tag}"
            git push --tags
        """
    }

    def resetAndClean() {
        Config.pipeline.sh """
            cd "${this.dir}"
            git reset --hard
            git clean -fd
        """
    }

    def fetchAndCheckoutBranch(String branch) {
        Config.pipeline.sh """
            cd "${this.dir}"
            git fetch origin "+refs/heads/${branch}:refs/remotes/origin/${branch}" --no-tags
            git checkout -B "${branch}" --track "origin/${branch}"
        """
        setBranch(branch)
    }

    def fetchAndCheckoutTag(String tag) {
        Config.pipeline.sh """
            git fetch origin "+refs/tags/${tag}:refs/tags/${tag}" --no-tags
            git checkout "${tag}"
        """
    }

    def fetchAndCheckoutCommit(String commit) {
        Config.pipeline.sh """
            cd "${this.dir}"
            git fetch origin --no-tags
            git checkout "${commit}"
        """
    }

    def pull() {
        Config.pipeline.sh """
            cd "${this.dir}"
            git add --all
            git stash clear
            git stash
            git pull
            if git stash show > /dev/null 2>&1; then
                git stash pop
            fi
        """
    }

    def commitAndPush(commitMessage) {
        Config.pipeline.sh(returnStdout: true, script: """
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
        Config.pipeline.sh """
            cd "${this.dir}"
            git push -f
        """
    }

    def rollback() {
        Config.pipeline.sh """
            cd "${this.dir}"
            git reset --hard HEAD~1
        """
    }

    def updateSubmodules() {
        Config.pipeline.sh """
            cd "${this.dir}"
            git submodule update --init --recursive
        """
    }

}
