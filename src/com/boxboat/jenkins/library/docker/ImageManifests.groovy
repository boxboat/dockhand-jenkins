package com.boxboat.jenkins.library.docker

import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.buildVersions.GitBuildVersions
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.git.GitRepo

class ImageManifests implements Serializable {

    Map<String, List<ImageManifest>> manifests = [:]

    Image image

    ImageManifests(Image image) {
        this.image = image
    }

    def addArtifactoryManifest(ArtifactoryImageManifest manifest) {
        String digest = manifest.digest
        if (!manifests[digest]) {
            manifests[digest] = []
        }
        manifests[digest].add(manifest)
    }

    def addDtrManifest(manifest) {
        if (DtrImageManifest.isValid(manifest)) {
            ImageManifest imageManifest = new DtrImageManifest(manifest)
            String digest = imageManifest.digest

            if (!manifests[digest]) {
                manifests[digest] = []
            }
            manifests[digest].add(imageManifest)
        }
    }

    def addHarborManifest(manifest) {
        if (HarborImageManifest.isValid(manifest)) {
            ImageManifest imageManifest = new HarborImageManifest(manifest)
            String digest = imageManifest.digest

            if (!manifests[digest]) {
                manifests[digest] = []
            }
            manifests[digest].add(imageManifest)
        }
    }

    List<String> getCleanableTagsList(retentionDays) {
        Date now = new Date()
        long nowSec = now.getTime()

        // would be nice to use a set here but I think Jenkins doesn't like sets
        Map<String, String> branchMap

        GitBuildVersions buildVersions = Config.getBuildVersions()
        def gitRepoPath = buildVersions.getImageRepoPath(image)
        if (gitRepoPath) {
            try {
                branchMap = GitRepo.remoteBranches(Config.getGitRemoteUrl(gitRepoPath)).collectEntries { it ->
                    [(Utils.cleanTag("commit/${it}")): it]
                }
            } catch (Exception ignored) {
                // the git remote may have moved
            }
        }

        List<String> cleanableTagsList = []
        manifests.keySet().toList().each { key ->
            def imageManifests = manifests[key]
            def candidateManifests = imageManifests.findAll { manifest ->
                manifest.isCommitHashTag() || (branchMap != null && manifest.isBranchTag() && !branchMap.containsKey(manifest.tag))
            }

            if (candidateManifests.size() == imageManifests.size()) {
                // all tags matching digest are commit hash tags or branch tags no longer on remote
                imageManifests.each { manifest ->
                    if (manifest.ageInDays(nowSec) > retentionDays) {
                        cleanableTagsList.add(manifest.tag)
                    }
                }
            }
        }

        return cleanableTagsList
    }

}
