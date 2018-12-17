package com.boxboat.jenkins.library.docker

import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.config.GlobalConfig
import groovy.json.JsonSlurper

class DTRClean implements Serializable {

    def registryAPIBase = '/api/v0'
    def retentionDays
    def dryRun

    DTRClean(dryRun = false, retentionDays = 15){
          this.dryRun = dryRun
          this.retentionDays = retentionDays
    }

    def readRepositories(Registry registry){
        def requestURL = registry.getRegistryUrl() + registryAPIBase + '/repositories?pageSize=100000&count=false'
        return Config.pipeline.httpRequest(
                url: requestURL,
                authentication: registry.credential,
                httpMode: 'GET',
                contentType: "APPLICATION_JSON"
        ).getContent()
    }
    def readRepositoryTags(Registry registry, def namespace, def name){
        def requestURI = registry.getRegistryUrl() + registryAPIBase + "/repositories/${namespace}/${name}/tags?pageSize=10000&count=false&includeManifests=false"
        return Config.pipeline.httpRequest(
                url: requestURI,
                authentication: registry.credential,
                httpMode: 'GET',
                contentType: "APPLICATION_JSON"
        ).getContent()
    }

    def deleteTag(Registry registry, def namespace, def name, def tag){
        Config.pipeline.echo "Removing ${namespace}/${name}:${tag}"
        if (!dryRun) {
            // clean up tags
            def requestURI = registry.getRegistryUrl() + registryAPIBase + "/repositories/${namespace}/${name}/tags/${tag}"
            Config.pipeline.httpRequest(
                url: requestURI,
                authentication: registry.credential,
                httpMode: 'DELETE',
                contentType: "APPLICATION_JSON"
            )
        }
    }

    def cleanRegistry( def registryName = "default") {
        def json = new JsonSlurper()
        ImageManifests imageManifests = new ImageManifests()
        Registry registry = Config.global.getRegistry(registryName)
        if (dryRun){
            Config.pipeline.echo "Dry Run"
        }
        def registryRepositories = json.parseText(readRepositories(registry))

        for (registryRepository in registryRepositories.repositories){
            def namespace = registryRepository.namespace
            def name = registryRepository.name

            Config.pipeline.echo "Reading ${name}/${namespace}"

            def registryRepositoryTags = json.parseText(readRepositoryTags(registry, namespace, name))


            for (registryRepositoryTag in registryRepositoryTags){
                  imageManifests.addManifest(registryRepositoryTag)
            }

            for (tag in imageManifests.getCleanableTagsList()) {
                deleteTag(registry, namespace, name, tag)
            }
        }
    }
}
