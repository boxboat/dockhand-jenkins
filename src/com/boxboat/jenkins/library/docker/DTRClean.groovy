package com.boxboat.jenkins.library.docker

import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.Config

class DTRClean implements Serializable {

    protected final String registryAPIBase = '/api/v0'

    boolean dryRun
    int retentionDays
    List<String> registryKeys

    LinkedHashMap<Object, Object> readRepositories(Registry registry) {
        def requestURL = registry.getRegistryUrl() + registryAPIBase + '/repositories?pageSize=100000&count=false'
        def result = Config.pipeline.httpRequest(
                url: requestURL,
                authentication: registry.credential,
                httpMode: 'GET',
                contentType: "APPLICATION_JSON"
        )?.getContent()

        def repositories = []
        if (result && result != "null") {
            repositories = Config.pipeline.readJSON(text: result.toString())
        }

        return Utils.resultOrTest(repositories, [
                "repositories": [
                    [
                      "id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                      "namespace": "apps",
                      "namespaceType": "organization",
                      "name": "test1",
                      "shortDescription": "",
                      "visibility": "private",
                      "scanOnPush": false,
                      "immutableTags": false,
                      "enableManifestLists": false,
                      "pulls": 5,
                      "pushes": 5,
                      "tagLimit": 0
                    ],
                    [
                      "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                      "namespace": "apps",
                      "namespaceType": "organization",
                      "name": "test2",
                      "shortDescription": "",
                      "visibility": "private",
                      "scanOnPush": false,
                      "immutableTags": false,
                      "enableManifestLists": false,
                      "pulls": 5,
                      "pushes": 5,
                      "tagLimit": 0
                    ]
                ]
            ])
    }

    List<Object> readRepositoryTags(Registry registry, String namespace, String name) {
        def requestURI = registry.getRegistryUrl() + registryAPIBase + "/repositories/${namespace}/${name}/tags?pageSize=10000&count=false&includeManifests=false"
        def result = Config.pipeline.httpRequest(
                url: requestURI,
                authentication: registry.credential,
                httpMode: 'GET',
                contentType: "APPLICATION_JSON"
        )?.getContent()

        def tags = []
        if (result && result != "null") {
            tags = Config.pipeline.readJSON(text: result.toString())
        }

        return Utils.resultOrTest(tags, [
              [
                "name": "commit-test",
                "digest": "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "author": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                "updatedAt": "2018-12-01T00:00:00.000Z",
                "createdAt": "2018-12-01T00:00:00.000Z",
                "hashMismatch": false,
                "inNotary": false,
                "manifest": [
                  "digest": "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                  "mediaType": "application/vnd.docker.distribution.manifest.v2+json",
                  "configMediaType": "application/vnd.docker.container.image.v1+json",
                  "size": 100,
                  "createdAt": "2018-12-01T00:00:00.000Z"
                ]
              ],
              [
                "name": "build-aaaaaaaaaaaa",
                "digest": "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "author": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                "updatedAt": "2018-12-01T00:00:00.000Z",
                "createdAt": "2018-12-01T00:00:00.000Z",
                "hashMismatch": false,
                "inNotary": false,
                "manifest": [
                  "digest": "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                  "mediaType": "application/vnd.docker.distribution.manifest.v2+json",
                  "configMediaType": "application/vnd.docker.container.image.v1+json",
                  "size": 100,
                  "createdAt": "2018-12-01T00:00:00.000Z"
                ]
              ],
              [
                "name": "build-bbbbbbbbbbbb",
                "digest": "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                "author": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                "updatedAt": "2018-12-01T00:00:00.000Z",
                "createdAt": "2018-12-01T00:00:00.000Z",
                "hashMismatch": false,
                "inNotary": false,
                "manifest": [
                  "digest": "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                  "mediaType": "application/vnd.docker.distribution.manifest.v2+json",
                  "configMediaType": "application/vnd.docker.container.image.v1+json",
                  "size": 100,
                  "createdAt": "2018-12-01T00:00:00.000Z"
                ]
              ],
              [
                "name": "build-cccccccccccc",
                "digest": "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                "author": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                "updatedAt": "2018-12-01T00:00:00.000Z",
                "createdAt": "2018-12-01T00:00:00.000Z",
                "hashMismatch": false,
                "inNotary": false,
                "manifest": [
                  "digest": "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                  "mediaType": "application/vnd.docker.distribution.manifest.v2+json",
                  "configMediaType": "application/vnd.docker.container.image.v1+json",
                  "size": 100,
                  "createdAt": "2018-12-01T00:00:00.000Z"
                ]
              ]
            ])
    }

    int deleteTag(Registry registry, String namespace, String name, String tag) {
        Config.pipeline.echo "Removing ${namespace}/${name}:${tag}"
        if (dryRun) {
            return 200
        }

        // clean up tags
        def requestURI = registry.getRegistryUrl() + registryAPIBase + "/repositories/${namespace}/${name}/tags/${tag}"
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
        registryRepositories.repositories.each { registryRepository ->
            String namespace = registryRepository.namespace
            String name = registryRepository.name

            Config.pipeline.echo "Reading ${name}/${namespace}"

            def registryRepositoryTags = readRepositoryTags(registry, namespace, name)

            def imageManifests = new ImageManifests()
            registryRepositoryTags.each { registryRepositoryTag ->
                imageManifests.addDtrManifest(registryRepositoryTag)
            }
            imageManifests.getCleanableTagsList().each { tag ->
                deleteTag(registry, namespace, name, tag)
            }

        }

    }

}
