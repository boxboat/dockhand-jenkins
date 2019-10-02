package com.boxboat.jenkins.library.docker

import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.Config
import groovy.json.JsonSlurper

class HarborRegistryClean implements Serializable {

    protected final String registryAPIBase = '/api'

    boolean dryRun
    int retentionDays
    List<String> registryKeys

    List<Object> readRepositories(Registry registry) {
        def requestURL = registry.getRegistryUrl() + registryAPIBase + '/projects?page_size=100'
        def result = Config.pipeline.httpRequest(
                url: requestURL,
                authentication: registry.credential,
                httpMode: 'GET',
                contentType: "APPLICATION_JSON"
        )?.getContent()

        def projects = Config.pipeline.readJSON(text: result.toString())

        def repositories = []

        projects.each { project ->
            requestURL = registry.getRegistryUrl() + registryAPIBase + "/repositories?project_id=${project.project_id}&page_size=100"
            result = Config.pipeline.httpRequest(
                    url: requestURL,
                    authentication: registry.credential,
                    httpMode: 'GET',
                    contentType: "APPLICATION_JSON"
            )?.getContent()
            def repo = Config.pipeline.readJSON(text: result.toString())
            repo.each { imageRepo ->
                repositories.add(imageRepo)
            }
        }
        return repositories

//        return Utils.resultOrTest(repositories,
//                [
//                        {
//                            "project_id" : "1" ,
//                            "owner_id" : "3" ,
//                            "name" : "test1" ,
//                            "creation_time" : "2019-01-01T16:00:00Z" ,
//                            "update_time" : "2019-01-01T16:00:00Z" ,
//                            "repo_count" : 1 ,
//                            "chart_count" : 0 ,
//                            "metadata" : {
//                            "public" : "true"
//                        }
//                        },
//                        {
//                            "project_id" : "2" ,
//                            "owner_id" : "3" ,
//                            "name" : "test2" ,
//                            "creation_time" : "2019-01-01T16:00:00Z" ,
//                            "update_time" : "2019-01-01T16:00:00Z" ,
//                            "repo_count" : 3 ,
//                            "chart_count" : 0 ,
//                            "metadata" : {
//                            "public" : "true"
//                        }
//                        }
//                ])
    }

    String readRepositoryTags(Registry registry, String name) {
        def requestURI = registry.getRegistryUrl() + registryAPIBase + "/repositories/${name}/tags"
        def result = Config.pipeline.httpRequest(
                url: requestURI,
                authentication: registry.credential,
                httpMode: 'GET',
                contentType: "APPLICATION_JSON"
        )?.getContent()
        return result
//        return Utils.resultOrTest(result, """
//            [
//              {
//                "name": "commit-test",
//                "digest": "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
//                "author": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
//                "updatedAt": "2018-12-01T00:00:00.000Z",
//                "createdAt": "2018-12-01T00:00:00.000Z",
//                "hashMismatch": false,
//                "inNotary": false,
//                "manifest": {
//                  "digest": "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
//                  "mediaType": "application/vnd.docker.distribution.manifest.v2+json",
//                  "configMediaType": "application/vnd.docker.container.image.v1+json",
//                  "size": 100,
//                  "createdAt": "2018-12-01T00:00:00.000Z"
//                }
//              },
//              {
//                "name": "build-aaaaaaaaaaaa",
//                "digest": "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
//                "author": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
//                "updatedAt": "2018-12-01T00:00:00.000Z",
//                "createdAt": "2018-12-01T00:00:00.000Z",
//                "hashMismatch": false,
//                "inNotary": false,
//                "manifest": {
//                  "digest": "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
//                  "mediaType": "application/vnd.docker.distribution.manifest.v2+json",
//                  "configMediaType": "application/vnd.docker.container.image.v1+json",
//                  "size": 100,
//                  "createdAt": "2018-12-01T00:00:00.000Z"
//                }
//              },
//              {
//                "name": "build-bbbbbbbbbbbb",
//                "digest": "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
//                "author": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
//                "updatedAt": "2018-12-01T00:00:00.000Z",
//                "createdAt": "2018-12-01T00:00:00.000Z",
//                "hashMismatch": false,
//                "inNotary": false,
//                "manifest": {
//                  "digest": "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
//                  "mediaType": "application/vnd.docker.distribution.manifest.v2+json",
//                  "configMediaType": "application/vnd.docker.container.image.v1+json",
//                  "size": 100,
//                  "createdAt": "2018-12-01T00:00:00.000Z"
//                }
//              },
//              {
//                "name": "build-cccccccccccc",
//                "digest": "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
//                "author": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
//                "updatedAt": "2018-12-01T00:00:00.000Z",
//                "createdAt": "2018-12-01T00:00:00.000Z",
//                "hashMismatch": false,
//                "inNotary": false,
//                "manifest": {
//                  "digest": "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
//                  "mediaType": "application/vnd.docker.distribution.manifest.v2+json",
//                  "configMediaType": "application/vnd.docker.container.image.v1+json",
//                  "size": 100,
//                  "createdAt": "2018-12-01T00:00:00.000Z"
//                }
//              }
//            ]
//        """)
    }

    int deleteTag(Registry registry, String name, String tag) {
        Config.pipeline.echo "Removing ${name}:${tag}"
        if (dryRun) {
            return 200
        }

        // clean up tags
        def requestURI = registry.getRegistryUrl() + registryAPIBase + "/repositories/${name}/tags/${tag}"
        def result = Config.pipeline.httpRequest(
                url: requestURI,
                authentication: registry.credential,
                httpMode: 'DELETE',
                contentType: "APPLICATION_JSON"
        )?.getStatus()
        return Utils.resultOrTest(result, 200)
    }

    def clean() {
        registryKeys.each { registryKey ->
            cleanRegistry(registryKey)
        }
    }

    protected cleanRegistry(String registryKey) {
        def registry = Config.global.getRegistry(registryKey)

        if (dryRun) {
            Config.pipeline.echo "Dry Run"
        }
        Config.pipeline.echo "RetentionDays: ${retentionDays}"

        def registryRepositories = readRepositories(registry)
        registryRepositories.each { registryRepository ->
            if (registryRepository) {
                String name = registryRepository.name

                Config.pipeline.echo "Reading ${name}"

                def registryRepositoryTags = Config.pipeline.readJSON(text: readRepositoryTags(registry, name))

                def imageManifests = new ImageManifests()
                registryRepositoryTags.each { registryRepositoryTag ->
                    imageManifests.addHarborManifest(registryRepositoryTag)
                }
                imageManifests.getCleanableTagsList().each { tag ->
                    deleteTag(registry, name, tag)
                }
            }

        }

    }

}
