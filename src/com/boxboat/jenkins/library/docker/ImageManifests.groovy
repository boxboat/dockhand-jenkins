package com.boxboat.jenkins.library.docker

import com.boxboat.jenkins.library.config.Config

class ImageManifests implements Serializable {

    Map<String, List<ImageManifest>> manifests = [:]

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

        List<String> cleanableTagsList = []
        manifests.keySet().toList().each { key ->
            def imageManifests = manifests[key]
            def commitHashManifests = imageManifests.findAll {
                manifest -> manifest.isCommitHash()
            }

            if (commitHashManifests.size() == imageManifests.size()) {
                // all tags matching digest are commit hash tags
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
