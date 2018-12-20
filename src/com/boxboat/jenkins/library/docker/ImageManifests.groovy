package com.boxboat.jenkins.library.docker

class ImageManifests implements Serializable {

    Map<String, List<ImageManifest>> manifests = [:]

    def addManifest(manifest) {
        if (manifest && manifest.name && manifest.updatedAt && manifest.digest) {
            ImageManifest imageManifest = new ImageManifest(manifest)
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
