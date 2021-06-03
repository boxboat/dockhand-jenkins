package com.boxboat.jenkins.library.buildVersions


import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.git.GitRepo

class GitBuildVersionsClean implements Serializable {

    boolean dryRun

    def clean() {
        List<String> gitRmCommands = []

        def buildVersions = new GitBuildVersions()
        buildVersions.checkout()

        // list all image repos
        // format <imageTextFile>=<repoPath>
        List<String> repoStrings = Config.pipeline.sh(returnStdout: true, script: '''
            cd build-versions
            find image-repos -type f -exec sh -c 'printf "$(basename "{}")="; head -n1 "{}"' \\; | sort
        ''')?.trim()?.split('\n')?.findAll { it -> !it.isEmpty() }

        if (!repoStrings) {
            Config.pipeline.echo "'image-repos' is empty; nothing to clean"
        }

        // iterate through each image repo
        repoStrings.each { repoString ->
            // parse imageTextFile and repoPath from strings
            List<String> repoStringSplit = repoString.trim()?.split('=')?.findAll { it -> !it.isEmpty() }
            def imageTextFile = repoStringSplit[0]
            if (repoStringSplit.size() < 2) {
                Config.pipeline.echo "'image-repos/${imageTextFile}' is empty; removing"
                gitRmCommands.add("git rm \"image-repos/${imageTextFile}\"")
                return
            }

            // check repoPath for valid remote URL
            def repoPath = repoStringSplit[1]
            def gitRemoteUrl = Config.getGitRemoteUrl(repoPath)
            if (!gitRemoteUrl) {
                Config.pipeline.echo "'image-repos/${imageTextFile}' repo path '${repoPath}' does not parse to a valid remote url; skipping"
                return
            }

            Map<String, String> remoteBranchMap
            try {
                remoteBranchMap = GitRepo.remoteBranches(gitRemoteUrl).collectEntries { it ->
                    [("image-versions/commit/${it}/${imageTextFile}".toString()): it]
                }
            } catch (Exception ignored) {
                Config.pipeline.echo "'image-repos/${imageTextFile}' remote '${gitRemoteUrl}' could not list remote branches; skipping"
                return
            }

            List<String> commitBranchVersions = Config.pipeline.sh(returnStdout: true, script: """
                cd build-versions
                find image-versions/commit -type f -name "${imageTextFile}" | sort;
            """)?.trim()?.split('\n')?.findAll { it -> !it.isEmpty() }

            if (!commitBranchVersions) {
                Config.pipeline.echo "'${imageTextFile}' remote '${gitRemoteUrl}' does not have any commit branch versions; skipping"
                return
            }

            commitBranchVersions.each { commitBranchVersion ->
                if (!remoteBranchMap.containsKey(commitBranchVersion)) {
                    Config.pipeline.echo "'${imageTextFile}' remote '${gitRemoteUrl}' removing '${commitBranchVersion}'"
                    gitRmCommands.add("git rm \"${commitBranchVersion}\"")
                }
            }
        }

        if (gitRmCommands) {
            gitRmCommands
            Config.pipeline.sh 'cd build-versions\n' + gitRmCommands.join('\n')
        } else {
            Config.pipeline.echo "no changes"
            return
        }

        if (dryRun) {
            Config.pipeline.echo "dry run enabled; git status:"
            Config.pipeline.sh """
                cd build-versions
                git status
            """
        } else {
            Config.pipeline.echo "saving build-versions"
            buildVersions.save()
        }
    }

}
