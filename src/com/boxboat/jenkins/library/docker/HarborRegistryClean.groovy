package com.boxboat.jenkins.library.docker

import com.boxboat.jenkins.library.Utils
import com.boxboat.jenkins.library.config.Config

class HarborRegistryClean implements Serializable {

    protected final String registryAPIBase = '/api/v2.0'

    boolean dryRun
    int retentionDays
    List<String> registryKeys

    private int pageSize = 100

    List<Map<String, Object>> requestProjects(Registry registry) {
        return Utils.resultOrTest(requestPaginated(registry, "/projects", pageSize),
                [
                        [
                                "project_id"   : 1,
                                "name"         : "proj1",
                                "creation_time": "2019-01-01T16:00:00.00Z",
                                "update_time"  : "2019-01-01T16:00:00.00Z",
                        ],
                        [
                                "project_id"   : 2,
                                "name"         : "proj2",
                                "creation_time": "2019-01-01T16:00:00.00Z",
                                "update_time"  : "2019-01-01T16:00:00.00Z",
                        ],
                ]
        )
    }

    List<Map<String, Object>> requestRepositories(Registry registry, String projectName) {
        return Utils.resultOrTest(requestPaginated(registry, "/projects/${URLEncoder.encode(projectName, 'UTF-8')}/repositories", pageSize),
                [
                        [
                                "id"           : "1",
                                "name"         : "proj1/repo1",
                                "creation_time": "2019-01-01T16:00:00.00Z",
                                "update_time"  : "2019-01-01T16:00:00.00Z",
                        ],
                        [
                                "id"           : "2",
                                "name"         : "proj1/repo2",
                                "creation_time": "2019-01-01T16:00:00.00Z",
                                "update_time"  : "2019-01-01T16:00:00.00Z",
                        ],
                ]
        )
    }

    List<Map<String, Object>> requestArtifacts(Registry registry, String projectName, String repositoryName) {
        return Utils.resultOrTest(requestPaginated(registry, "/projects/${URLEncoder.encode(projectName, 'UTF-8')}/repositories/${URLEncoder.encode(repositoryName, 'UTF-8')}/artifacts", pageSize, [q: "tags=*"]),
                [
                        [
                                "id"    : 1,
                                "digest": "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                                "tags"  : [
                                        [
                                                "id"       : 1,
                                                "name"     : "build-aaaaaaaaaaaa",
                                                "push_time": "2019-01-01T16:00:00.00Z",
                                        ],
                                        [
                                                "id"       : 2,
                                                "name"     : "commit-master",
                                                "push_time": "2019-01-01T16:00:00.00Z",
                                        ],
                                ],
                        ],
                        [
                                "id"    : 2,
                                "digest": "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                                "tags"  : [
                                        [
                                                "id"       : 3,
                                                "name"     : "build-bbbbbbbbbbbb",
                                                "push_time": "2019-01-01T16:00:00.00Z",
                                        ],
                                ],
                        ],
                ]
        )
    }

    int deleteTag(Registry registry, String projectName, String repositoryName, String tag) {
        Config.pipeline.echo "Removing ${projectName}/${repositoryName}:${tag}"
        if (dryRun) {
            return 200
        }
        def requestURI = registry.getRegistryUrl() + registryAPIBase + "/projects/${projectName}/repositories/${repositoryName}/artifacts/${tag}/tags/${tag}"
        def result
        registry.withCredentials([skipDockerConfig: true]) {
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

        requestProjects(registry).each { project ->
            String projectName = project.name
            requestRepositories(registry, projectName).each { repository ->
                String projectAndRepositoryName = repository.name
                String repositoryName = repository.name.substring(project.name.size() + 1)
                def imageManifests = new ImageManifests(new Image(path: projectAndRepositoryName))
                requestArtifacts(registry, projectName, repositoryName).each { artifact ->
                    artifact.tags.each { tag ->
                        imageManifests.addHarborManifest([digest: artifact.digest] << tag)
                    }
                }
                imageManifests.getCleanableTagsList(retentionDays).each { tag ->
                    deleteTag(registry, projectName, repositoryName, tag)
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
            registry.withCredentials([skipDockerConfig: true]) {
                def auth = Config.pipeline.env["REGISTRY_USERNAME"] + ":" + Config.pipeline.env["REGISTRY_PASSWORD"]
                def encoded = auth.bytes.encodeBase64().toString()
                pResponse = Config.pipeline.httpRequest(
                        url: requestURL + "?" + paginatedQuery.collect { k, v -> "$k=${URLEncoder.encode(v, 'UTF-8')}" }.join('&'),
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
