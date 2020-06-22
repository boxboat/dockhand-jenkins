package com.boxboat.jenkins.library.git

import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.Config

class GitRepo implements Serializable {

    public dir

    private shortHashLength = 12

    static List<String> remoteBranches(String gitRemoteUrl) {
        return Utils.resultOrTest(Config.pipeline.sh(returnStdout: true, script: """
            git ls-remote --heads "${gitRemoteUrl}" | sed 's|.*refs/heads/\\(.*\\)|\\1|g'
        """)?.trim()?.split('\n')?.findAll { it -> !it.isEmpty() }, ["master", "develop"])
    }

    String getHash() {
        return Utils.resultOrTest(Config.pipeline.sh(returnStdout: true, script: """
            cd "${this.dir}"
            git show -s --format=%H
        """)?.trim(), "0123456789abcdef0123456789abcdef")
    }

    String getShortHash() {
        return getHash()?.substring(0, shortHashLength)
    }

    String _branch
    String _prBranch

    String getPrBranch(){
        return _prBranch
    }

    String getBranch() {
        if (!_branch) {
            _branch = Config.pipeline.sh(returnStdout: true, script: "git rev-parse --abbrev-ref HEAD")?.trim()
        }
        return Utils.resultOrTest(_branch, "master")
    }

    String setBranch(String value) {
        _branch = value
        if (_branch.startsWith("origin/")) {
            _branch = _branch.substring("origin/".length())
        }
    }

    String setPrBranch(String value){
        _prBranch = value
        if (_prBranch.startsWith("origin/")) {
            _prBranch = _prBranch.substring("origin/".length())
        }
    }

    String getRemoteUrl() {
        def result = Config.pipeline.sh(returnStdout: true, script: """
            cd "${this.dir}"
            git config remote.origin.url
        """)?.trim()
        return Utils.resultOrTest(result, Config.global.git.buildVersionsUrl)
    }

    String getRemotePath() {
        return Config.getGitRemotePath(getRemoteUrl())
    }

    String getCommitUrl() {
        return Config.getGitSelected().getCommitUrl(getRemotePath(), getHash())
    }

    String getBranchUrl() {
        return Config.getGitSelected().getBranchUrl(getRemotePath(), getBranch())
    }

    String getTagReferenceHash(String tag) {
        def result = Config.pipeline.sh(returnStdout: true, script: """
            cd "${this.dir}"
            git show-ref --hash=${shortHashLength} -s ${tag}  || :
        """)?.trim()
        return Utils.resultOrTest(result, "0123456789abcdef0123456789abcdef".substring(shortHashLength))
    }

    boolean isBranchTip() {
        String originHash = Config.pipeline.sh(returnStdout: true, script: """
            git show-branch --sha1-name origin/${this.getBranch()} || :
        """)
        String tipHash
        def closure = {
            def matcher = originHash =~ /^\[([0-9a-f]+)\]/
            tipHash = matcher.hasGroup() && matcher.size() > 0 ? matcher[0][1] : null
        }
        closure()
        def result = false
        if (tipHash) {
            result = hash.startsWith(tipHash)
        }
        return Utils.resultOrTest(result, true)
    }

    boolean isBranchPullRequest() {
        return _prBranch != null
    }

    def checkout(String checkout) {
        // git clean must be executed inside Config.pipeline.dir block
        Config.pipeline.dir(dir) {
            Config.pipeline.sh """
                git checkout "${checkout}"
                git reset --hard
                git clean -ffd
            """
        }
    }

    boolean currentBranchContainsCommit(String commit) {
        def containsCommit = Config.pipeline.sh(returnStdout: true, script: """
            git branch ${this.getBranch()} --contains "${commit}"
        """)?.trim()
        return containsCommit ? true : false
    }

    def resetToHash(String commitHash) {
        // git clean must be executed inside Config.pipeline.dir block
        Config.pipeline.dir(dir) {
            Config.pipeline.sh """
                git reset --hard "${commitHash}"
                git clean -ffd
            """
        }
    }

    def tagAndPush(String tag) {
        Config.pipeline.sh """
            cd "${this.dir}"
            git tag -f "${tag}"
            git push origin "refs/tags/${tag}"
        """
    }

    def resetAndClean() {
        // git clean must be executed inside Config.pipeline.dir block
        Config.pipeline.dir(dir) {
            Config.pipeline.sh """
                git reset --hard
                git clean -ffd
            """
        }
    }

    // Requires 'Checkout over SSH' setting in Jenkins
    def fetchAndCheckoutBranch(String branch) {
        Config.pipeline.sh """
            cd "${this.dir}"
            git fetch origin "+refs/heads/${branch}:refs/remotes/origin/${branch}" --no-tags
            git checkout -B "${branch}" "origin/${branch}"
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
                git stash pop || grep -lr '<<<<<<<' . | xargs git checkout --theirs
            fi
        """
    }

    boolean hasChanges() {
        def rc = Config.pipeline.sh(returnStatus: true, script: """
            cd "${this.dir}"
            git add --all
            git update-index -q --refresh
            git diff-index --quiet HEAD --
        """)
        return rc != 0
    }

    def commitAndPush(commitMessage) {
        Config.pipeline.sh """
            cd "${this.dir}"
            git add --all
            git commit -m "${commitMessage}"
            git push
        """
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
