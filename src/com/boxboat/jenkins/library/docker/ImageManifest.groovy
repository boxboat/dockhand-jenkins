package com.boxboat.jenkins.library.docker

class ImageManifest implements Serializable {
    String tag
    Date lastUpdated
    String digest

    ImageManifest(def data){
        tag = data.name
        digest = data.digest

        lastUpdated = Date.parse('yyyy-MM-dd', data.updatedAt.substring(0, 10))
    }

    Integer ageInDays(long now){
        int secsToDays = 86400000
        long imageAge = lastUpdated.getTime()
        return (now - imageAge) / secsToDays
    }

    Boolean isCommitHash(){
        return tag.matches(/(?i)^build-[0-9a-f]{12}$/)
    }
}
