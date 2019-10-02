package com.boxboat.jenkins.library.docker

class ImageManifest implements Serializable {

    String tag
    Date lastUpdated
    String digest

    ImageManifest(String tag, String digest, Date lastUpdated) {
        this.tag = tag
        this.digest = digest
        this.lastUpdated = lastUpdated
    }

    int ageInDays(long now) {
        int secsToDays = 86400000
        long imageAge = lastUpdated.getTime()
        return (now - imageAge) / secsToDays
    }

    boolean isCommitHash() {
        return tag.matches(/(?i)^build-[0-9a-f]{12}$/)
    }
}
