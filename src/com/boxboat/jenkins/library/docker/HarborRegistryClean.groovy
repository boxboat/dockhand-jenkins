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

    List<Object> requestProjects(Registry registry, int page) {
        def requestURL = registry.getRegistryUrl() + registryAPIBase + "/projects?page_size=${projectPageSize}&page=${page}"
        def result = Config.pipeline.httpRequest(
                url: requestURL,
                authentication: registry.credential,
                httpMode: 'GET',
                contentType: "APPLICATION_JSON"
        )?.getContent()
        def projects = []
        if (result && result.toString() != "null") {
            projects = Config.pipeline.readJSON(text: result.toString())
        }
        if (projects.size() < projectPageSize) {
            return projects
        } else {
            projects.addAll(requestProjects(registry, page + 1))
            return projects
        }
    }

    List<Object> requestRepositories(Registry registry, int projectId, int page) {
        def requestURL = registry.getRegistryUrl() + registryAPIBase + "/repositories?project_id=${projectId}&=${repositoryPageSize}&page=${page}"
        def result = Config.pipeline.httpRequest(
                url: requestURL,
                authentication: registry.credential,
                httpMode: 'GET',
                contentType: "APPLICATION_JSON"
        )?.getContent()
        def repositories = []
        if (result && result.toString() != "null") {
            repositories =  Config.pipeline.readJSON(text: result.toString())
        }
        if (repositories.size() < repositoryPageSize){
            return repositories
        }
        else {
            repositories.addAll(requestRepositories(registry, projectId, page + 1))
            return repositories
        }
    }

    List<Object> readRepositories(Registry registry) {

        def projects = requestProjects(registry, 1)
        Config.pipeline.echo "projects: ${projects}"

        def repositories = []

        projects.each { project ->
            def projectRepos = requestRepositories(registry, project.project_id, 1)
            projectRepos.each { repo ->
                repositories.add(repo)
            }
        }

        return Utils.resultOrTest(repositories,
            [
                [
                    "id" : "1",
                    "project_id" : "1",
                    "name" : "test1",
                    "description" : "",
                    "creation_time" : "2019-01-01T16:00:00Z",
                    "update_time" : "2019-01-01T16:00:00Z",
                    "labels" : [],
                    "tags_count" : "1",
                    "star_count" : "0",
                ],
                [
                    "id" : "2",
                    "project_id" : "2",
                    "name" : "test2",
                    "description" : "",
                    "creation_time" : "2019-01-01T16:00:00Z",
                    "update_time" : "2019-01-01T16:00:00Z",
                    "labels" : [],
                    "tags_count" : "1",
                    "star_count" : "0",
                ],
            ]
        )
    }

    String readRepositoryTags(Registry registry, String name) {
        def requestURI = registry.getRegistryUrl() + registryAPIBase + "/repositories/${name}/tags"
        def result = Config.pipeline.httpRequest(
                url: requestURI,
                authentication: registry.credential,
                httpMode: 'GET',
                contentType: "APPLICATION_JSON"
        )?.getContent()
        return Utils.resultOrTest(result, """
            [
              {
                "digest": "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "name": "build-aaaaaaaaaaaa",
                "size": 232664445,
                "architecture": "amd64",
                "os": "linux",
                "os.version": "",
                "docker_version": "18.06.1-ce",
                "author": "",
                "created": "2019-09-09T19:41:11.917513133Z",
                "config": {
                  "labels": []
                },
                "signature": null,
                "scan_overview": {
                  "image_digest": "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                  "scan_status": "finished",
                  "job_id": 407,
                  "severity": 5,
                  "components": {
                    "total": 111,
                    "summary": [
                      {
                        "severity": 1,
                        "count": 76
                      },
                      {
                        "severity": 5,
                        "count": 12
                      },
                      {
                        "severity": 4,
                        "count": 17
                      },
                      {
                        "severity": 3,
                        "count": 6
                      }
                    ]
                  },
                  "details_key": "9250d0e8bf02011810a2fcc85ee87567fde1e0f09fe3548a138ee20e9196a034",
                  "creation_time": "2019-09-10T00:00:02.706191Z",
                  "update_time": "2019-10-02T00:00:04.812058Z"
                },
                "labels": [],
                "push_time": "2019-09-20T12:25:36.423777Z",
                "pull_time": "0001-01-01T00:00:00Z"
              },
              {
                "digest": "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "name": "commit-test",
                "size": 232664445,
                "architecture": "amd64",
                "os": "linux",
                "os.version": "",
                "docker_version": "18.06.1-ce",
                "author": "",
                "created": "2019-09-09T19:41:11.917513133Z",
                "config": {
                  "labels": []
                },
                "signature": null,
                "scan_overview": {
                  "image_digest": "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                  "scan_status": "finished",
                  "job_id": 407,
                  "severity": 5,
                  "components": {
                    "total": 111,
                    "summary": [
                      {
                        "severity": 1,
                        "count": 76
                      },
                      {
                        "severity": 5,
                        "count": 12
                      },
                      {
                        "severity": 4,
                        "count": 17
                      },
                      {
                        "severity": 3,
                        "count": 6
                      }
                    ]
                  },
                  "details_key": "9250d0e8bf02011810a2fcc85ee87567fde1e0f09fe3548a138ee20e9196a034",
                  "creation_time": "2019-09-10T00:00:02.706191Z",
                  "update_time": "2019-10-02T00:00:04.812058Z"
                },
                "labels": [],
                "push_time": "2019-09-20T12:25:36.423777Z",
                "pull_time": "0001-01-01T00:00:00Z"
              },
              {
                "digest": "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                "name": "commit-bbbbbbbbbbbb",
                "size": 232664445,
                "architecture": "amd64",
                "os": "linux",
                "os.version": "",
                "docker_version": "18.06.1-ce",
                "author": "",
                "created": "2019-09-09T19:41:11.917513133Z",
                "config": {
                  "labels": []
                },
                "signature": null,
                "scan_overview": {
                  "image_digest": "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                  "scan_status": "finished",
                  "job_id": 407,
                  "severity": 5,
                  "components": {
                    "total": 111,
                    "summary": [
                      {
                        "severity": 1,
                        "count": 76
                      },
                      {
                        "severity": 5,
                        "count": 12
                      },
                      {
                        "severity": 4,
                        "count": 17
                      },
                      {
                        "severity": 3,
                        "count": 6
                      }
                    ]
                  },
                  "details_key": "9250d0e8bf02011810a2fcc85ee87567fde1e0f09fe3548a138ee20e9196a034",
                  "creation_time": "2019-09-10T00:00:02.706191Z",
                  "update_time": "2019-10-02T00:00:04.812058Z"
                },
                "labels": [],
                "push_time": "2019-09-20T12:25:36.423777Z",
                "pull_time": "0001-01-01T00:00:00Z"
              },
              {
                "digest": "sha256:c",
                "name": "commit-cccccccccccc",
                "size": 232664445,
                "architecture": "amd64",
                "os": "linux",
                "os.version": "",
                "docker_version": "18.06.1-ce",
                "author": "",
                "created": "2019-09-09T19:41:11.917513133Z",
                "config": {
                  "labels": []
                },
                "signature": null,
                "scan_overview": {
                  "image_digest": "sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
                  "scan_status": "finished",
                  "job_id": 407,
                  "severity": 5,
                  "components": {
                    "total": 111,
                    "summary": [
                      {
                        "severity": 1,
                        "count": 76
                      },
                      {
                        "severity": 5,
                        "count": 12
                      },
                      {
                        "severity": 4,
                        "count": 17
                      },
                      {
                        "severity": 3,
                        "count": 6
                      }
                    ]
                  },
                  "details_key": "9250d0e8bf02011810a2fcc85ee87567fde1e0f09fe3548a138ee20e9196a034",
                  "creation_time": "2019-09-10T00:00:02.706191Z",
                  "update_time": "2019-10-02T00:00:04.812058Z"
                },
                "labels": [],
                "push_time": "2019-09-20T12:25:36.423777Z",
                "pull_time": "0001-01-01T00:00:00Z"
              }
            ]
        """)
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
