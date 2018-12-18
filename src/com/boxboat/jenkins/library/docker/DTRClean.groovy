package com.boxboat.jenkins.library.docker

import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.config.GlobalConfig
import groovy.json.JsonSlurper

class DTRClean implements Serializable {

    protected final String registryAPIBase = '/api/v0'
    Boolean dryRun
    Integer retentionDays

    String readRepositories(Registry registry){
        def requestURL = registry.getRegistryUrl() + registryAPIBase + '/repositories?pageSize=100000&count=false'
        return Config.pipeline.httpRequest(
                url: requestURL,
                authentication: registry.credential,
                httpMode: 'GET',
                contentType: "APPLICATION_JSON"
        ).getContent()
    }
    String readRepositoryTags(Registry registry, def namespace, def name){
        def requestURI = registry.getRegistryUrl() + registryAPIBase + "/repositories/${namespace}/${name}/tags?pageSize=10000&count=false&includeManifests=false"
        return Config.pipeline.httpRequest(
                url: requestURI,
                authentication: registry.credential,
                httpMode: 'GET',
                contentType: "APPLICATION_JSON"
        ).getContent()
    }

    Integer deleteTag(Registry registry, def namespace, def name, def tag){
        Config.pipeline.echo "Removing ${namespace}/${name}:${tag}"
        int status = 200
        if (!dryRun) {
            // clean up tags
            def requestURI = registry.getRegistryUrl() + registryAPIBase + "/repositories/${namespace}/${name}/tags/${tag}"
            status = Config.pipeline.httpRequest(
                url: requestURI,
                authentication: registry.credential,
                httpMode: 'DELETE',
                contentType: "APPLICATION_JSON"
            ).getStatus()
        }
        return status
    }

    def cleanRegistry( def registryName = "default") {
        def json = new JsonSlurper()
        ImageManifests imageManifests = new ImageManifests()
        Registry registry = Config.global.getRegistry(registryName)
        if (dryRun){
            Config.pipeline.echo "Dry Run"
        }
        Config.pipeline.echo "RetentionDays: ${retentionDays}"
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
