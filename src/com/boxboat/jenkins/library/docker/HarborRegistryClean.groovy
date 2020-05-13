package com.boxboat.jenkins.library.docker

import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.Config

class HarborRegistryClean implements Serializable {

    protected final String registryAPIBase = '/api'

    boolean dryRun
    int retentionDays
    List<String> registryKeys

    private int projectPageSize = 100
    private int repositoryPageSize = 100

    List<Map<String, Object>> requestProjects(Registry registry) {
        return requestPaginated(registry, "/projects", projectPageSize)
    }

    List<Map<String, Object>> requestRepositories(Registry registry, String projectId) {
        return requestPaginated(registry, "/repositories", repositoryPageSize, ["project_id": projectId])
    }

    List<Map<String, Object>> readRepositories(Registry registry) {

        def projects = requestProjects(registry)

        def repositories = []

        projects.each { project ->
            def projectRepos = requestRepositories(registry, project.project_id.toString())
            projectRepos.each { repo ->
                repositories.add(repo)
            }
        }

        return Utils.resultOrTest(repositories,
                [
                        [
                                "id"           : "1",
                                "project_id"   : "1",
                                "name"         : "test1",
                                "description"  : "",
                                "creation_time": "2019-01-01T16:00:00Z",
                                "update_time"  : "2019-01-01T16:00:00Z",
                                "labels"       : [],
                                "tags_count"   : "1",
                                "star_count"   : "0",
                        ],
                        [
                                "id"           : "2",
                                "project_id"   : "2",
                                "name"         : "test2",
                                "description"  : "",
                                "creation_time": "2019-01-01T16:00:00Z",
                                "update_time"  : "2019-01-01T16:00:00Z",
                                "labels"       : [],
                                "tags_count"   : "1",
                                "star_count"   : "0",
                        ],
                ]
        )
    }

    List<Map<String, Object>> readRepositoryTags(Registry registry, String name) {
        def requestURI = registry.getRegistryUrl() + registryAPIBase + "/repositories/${name}/tags"
        def result
        registry.withCredentials {
            def auth = Config.pipeline.env["REGISTRY_USERNAME"] + ":" + Config.pipeline.env["REGISTRY_PASSWORD"]
            def encoded = auth.bytes.encodeBase64().toString()
            result = Config.pipeline.httpRequest(
                    url: requestURI,
                    httpMode: 'GET',
                    contentType: "APPLICATION_JSON",
                    customHeaders: [[name: 'Authorization', value: "Basic ${encoded}"]]
            )?.getContent()
        }
        def tags = []
        if (result && result != "null") {
            tags = Config.pipeline.readJSON(text: result.toString())
        }
        return Utils.resultOrTest(tags, [
                [
                        "digest"        : "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        "name"          : "build-aaaaaaaaaaaa",
                        "size"          : 232664445,
                        "architecture"  : "amd64",
                        "os"            : "linux",
                        "os.version"    : "",
                        "docker_version": "18.06.1-ce",
                        "author"        : "",
                        "created"       : "2019-09-09T19:41:11.917513133Z",
                        "config"        : [
                                "labels": []
                        ],
                        "signature"     : null,
                        "scan_overview" : [
                                "image_digest" : "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                                "scan_status"  : "finished",
                                "job_id"       : 407,
                                "severity"     : 5,
                                "components"   : [
                                        "total"  : 111,
                                        "summary": [
                                                [
                                                        "severity": 1,
                                                        "count"   : 76
                                                ],
                                                [
                                                        "severity": 5,
                                                        "count"   : 12
                                                ],
                                                [
                                                        "severity": 4,
                                                        "count"   : 17
                                                ],
                                                [
                                                        "severity": 3,
                                                        "count"   : 6
                                                ]
                                        ]
                                ],
                                "details_key"  : "9250d0e8bf02011810a2fcc85ee87567fde1e0f09fe3548a138ee20e9196a034",
                                "creation_time": "2019-09-10T00:00:02.706191Z",
                                "update_time"  : "2019-10-02T00:00:04.812058Z"
                        ],
                        "labels"        : [],
                        "push_time"     : "2019-09-20T12:25:36.423777Z",
                        "pull_time"     : "0001-01-01T00:00:00Z"
                ],
                [
                        "digest"        : "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        "name"          : "commit-test",
                        "size"          : 232664445,
                        "architecture"  : "amd64",
                        "os"            : "linux",
                        "os.version"    : "",
                        "docker_version": "18.06.1-ce",
                        "author"        : "",
                        "created"       : "2019-09-09T19:41:11.917513133Z",
                        "config"        : [
                                "labels": []
                        ],
                        "signature"     : null,
                        "scan_overview" : [
                                "image_digest" : "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                                "scan_status"  : "finished",
                                "job_id"       : 407,
                                "severity"     : 5,
                                "components"   : [
                                        "total"  : 111,
                                        "summary": [
                                                [
                                                        "severity": 1,
                                                        "count"   : 76
                                                ],
                                                [
                                                        "severity": 5,
                                                        "count"   : 12
                                                ],
                                                [
                                                        "severity": 4,
                                                        "count"   : 17
                                                ],
                                                [
                                                        "severity": 3,
                                                        "count"   : 6
                                                ]
                                        ]
                                ],
                                "details_key"  : "9250d0e8bf02011810a2fcc85ee87567fde1e0f09fe3548a138ee20e9196a034",
                                "creation_time": "2019-09-10T00:00:02.706191Z",
                                "update_time"  : "2019-10-02T00:00:04.812058Z"
                        ],
                        "labels"        : [],
                        "push_time"     : "2019-09-20T12:25:36.423777Z",
                        "pull_time"     : "0001-01-01T00:00:00Z"
                ],
                [
                        "digest"        : "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                        "name"          : "commit-bbbbbbbbbbbb",
                        "size"          : 232664445,
                        "architecture"  : "amd64",
                        "os"            : "linux",
                        "os.version"    : "",
                        "docker_version": "18.06.1-ce",
                        "author"        : "",
                        "created"       : "2019-09-09T19:41:11.917513133Z",
                        "config"        : [
                                "labels": []
                        ],
                        "signature"     : null,
                        "scan_overview" : [
                                "image_digest" : "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                                "scan_status"  : "finished",
                                "job_id"       : 407,
                                "severity"     : 5,
                                "components"   : [
                                        "total"  : 111,
                                        "summary": [
                                                [
                                                        "severity": 1,
                                                        "count"   : 76
                                                ],
                                                [
                                                        "severity": 5,
                                                        "count"   : 12
                                                ],
                                                [
                                                        "severity": 4,
                                                        "count"   : 17
                                                ],
                                                [
                                                        "severity": 3,
                                                        "count"   : 6
                                                ]
                                        ]
                                ],
                                "details_key"  : "9250d0e8bf02011810a2fcc85ee87567fde1e0f09fe3548a138ee20e9196a034",
                                "creation_time": "2019-09-10T00:00:02.706191Z",
                                "update_time"  : "2019-10-02T00:00:04.812058Z"
                        ],
                        "labels"        : [],
                        "push_time"     : "2019-09-20T12:25:36.423777Z",
                        "pull_time"     : "0001-01-01T00:00:00Z"
                ],
                [
                        "digest"        : "sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
                        "name"          : "commit-cccccccccccc",
                        "size"          : 232664445,
                        "architecture"  : "amd64",
                        "os"            : "linux",
                        "os.version"    : "",
                        "docker_version": "18.06.1-ce",
                        "author"        : "",
                        "created"       : "2019-09-09T19:41:11.917513133Z",
                        "config"        : [
                                "labels": []
                        ],
                        "signature"     : null,
                        "scan_overview" : [
                                "image_digest" : "sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
                                "scan_status"  : "finished",
                                "job_id"       : 407,
                                "severity"     : 5,
                                "components"   : [
                                        "total"  : 111,
                                        "summary": [
                                                [
                                                        "severity": 1,
                                                        "count"   : 76
                                                ],
                                                [
                                                        "severity": 5,
                                                        "count"   : 12
                                                ],
                                                [
                                                        "severity": 4,
                                                        "count"   : 17
                                                ],
                                                [
                                                        "severity": 3,
                                                        "count"   : 6
                                                ]
                                        ]
                                ],
                                "details_key"  : "9250d0e8bf02011810a2fcc85ee87567fde1e0f09fe3548a138ee20e9196a034",
                                "creation_time": "2019-09-10T00:00:02.706191Z",
                                "update_time"  : "2019-10-02T00:00:04.812058Z"
                        ],
                        "labels"        : [],
                        "push_time"     : "2019-09-20T12:25:36.423777Z",
                        "pull_time"     : "0001-01-01T00:00:00Z"
                ]
        ])
    }

    int deleteTag(Registry registry, String name, String tag) {
        Config.pipeline.echo "Removing ${name}:${tag}"
        if (dryRun) {
            return 200
        }
        def requestURI = registry.getRegistryUrl() + registryAPIBase + "/repositories/${name}/tags/${tag}"
        def result
        registry.withCredentials {
            def auth = Config.pipeline.env["REGISTRY_USERNAME"] + ":" + Config.pipeline.env["REGISTRY_PASSWORD"]
            def encoded = auth.bytes.encodeBase64().toString()
            result = Config.pipeline.httpRequest(
                    url: requestURI,
                    httpMode: 'DELETE',
                    contentType: "APPLICATION_JSON",
                    customHeaders: [[name: 'Authorization', value: "Basic ${encoded}"]]
            )?.getStatus()
        }
        // clean up tags
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

                def registryRepositoryTags = readRepositoryTags(registry, name)

                def imageManifests = new ImageManifests(new Image(path: name))
                registryRepositoryTags.each { registryRepositoryTag ->
                    imageManifests.addHarborManifest(registryRepositoryTag)
                }
                imageManifests.getCleanableTagsList(retentionDays).each { tag ->
                    deleteTag(registry, name, tag)
                }
            }
        }
    }

    private List<Map<String, Object>> requestPaginated(
            Registry registry,
            String path,
            int pageSize,
            Map<String, String> query = [:]) {

        def requestURL = registry.getRegistryUrl() + registryAPIBase + path
        query["page_size"] = pageSize.toString()

        def result = []
        def pResultCount = pageSize

        for (def page = 1; pResultCount == pageSize; page++) {
            def paginatedQuery = query.clone()
            paginatedQuery["page"] = page.toString()

            def pResponse
            registry.withCredentials {
                def auth = Config.pipeline.env["REGISTRY_USERNAME"] + ":" + Config.pipeline.env["REGISTRY_PASSWORD"]
                def encoded = auth.bytes.encodeBase64().toString()
                pResponse = Config.pipeline.httpRequest(
                        url: requestURL + "?" + paginatedQuery.collect { k, v -> "$k=$v" }.join('&'),
                        httpMode: 'GET',
                        contentType: "APPLICATION_JSON",
                        customHeaders: [[name: 'Authorization', value: "Basic ${encoded}"]]
                )?.getContent()
            }

            def pResult = []
            if (pResponse && pResponse.toString() != "null") {
                pResult = Config.pipeline.readJSON(text: pResponse.toString())
            }

            result.addAll(pResult)
            pResultCount = pResult.size()
        }

        return result
    }


}
