package com.boxboat.jenkins.library.docker

class Registry implements Serializable {

    String scheme

    String host

    String credentials

    Registry(String host, String scheme, String credentials){
        this.host = host
        this.scheme = scheme
        this.credentials = credentials
    }

    def getRegistryUrl() {
        return "${scheme}://${host}"
    }

}
