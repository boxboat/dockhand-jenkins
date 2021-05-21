package com.boxboat.jenkins.library.docker

import com.boxboat.jenkins.library.config.Config

class ArtifactoryClean implements Serializable {

    boolean dryRun
    int retentionDays
    int removalCount = 0
    List<String> registryKeys
    String dockerRepoPathMatch
    String dockerRepo
    String cleanSummary

    Map<String, List<ArtifactoryImageManifest>> readRepositories(Registry registry) {
        def requestURI = registry.getRegistryUrl() + "/artifactory/api/search/aql"
        def findString = """items.find({"path":{"\$ne":"."},"\$or":[{"\$and":[{"repo":"${dockerRepo}","path":{"\$match":"${dockerRepoPathMatch}"},"name":"manifest.json"}]}]}).include("name","repo","path","actual_md5","actual_sha1","size","type","modified","created","property")"""
        Config.pipeline.echo "${requestURI}"
        Config.pipeline.echo "${findString}"
        def result = null
        registry.withCredentials {
            def auth = Config.pipeline.env["REGISTRY_USERNAME"] + ":" + Config.pipeline.env["REGISTRY_PASSWORD"]
            def encoded = auth.bytes.encodeBase64().toString()
            result = Config.pipeline.httpRequest(
                    url: requestURI,
                    httpMode: 'POST',
                    contentType: "TEXT_PLAIN",
                    customHeaders: [[name: 'Authorization', value: "Basic ${encoded}"]],
                    requestBody: findString
            )?.getContent()
        }
        def manifests = [:] as Map<String, Object>
        if (result && result != "null") {
            manifests = Config.pipeline.readJSON(text: result.toString())
        }

        def repos = new HashMap<String, List<ArtifactoryImageManifest>>()
        manifests.results.each { manifest ->
            def tagData = [
                    name     : "",
                    digest   : "",
                    updatedAt: manifest.modified
            ]
            def repoName = ""
            manifest.getAt("properties").each { prop ->
                if (prop.key == "docker.manifest.digest") {
                    tagData.digest = prop.value
                } else if (prop.key == "docker.repoName") {
                    repoName = prop.value
                } else if (prop.key == "docker.manifest") {
                    tagData.name = prop.value
                }
            }
            if (!repos.containsKey(repoName)) {
                repos[repoName] = new ArrayList<>()
            }

            if (ArtifactoryImageManifest.isValid(tagData)) {
                repos[repoName].add(new ArtifactoryImageManifest(tagData))
            }
        }

        return repos
    }

    int deleteTag(Registry registry, String name, String tag) {
        def result = -1
        Config.pipeline.echo "Removing ${name}:${tag}"
        if (dryRun) {
            cleanSummary = "${cleanSummary}\n${name}:${tag}"
            removalCount++
            return 200
        }

        // clean up tags
        def requestURI = registry.getRegistryUrl() + "/artifactory/${dockerRepo}/${name}/${tag}"
        registry.withCredentials {
            def auth = Config.pipeline.env["REGISTRY_USERNAME"] + ":" + Config.pipeline.env["REGISTRY_PASSWORD"]
            def encoded = auth.bytes.encodeBase64().toString()
            result = Config.pipeline.httpRequest(
                    url: requestURI,
                    httpMode: 'DELETE',
                    contentType: "APPLICATION_JSON",
                    customHeaders: [[name: 'Authorization', value: "Basic ${encoded}"]],
            )?.getContent()
        }
        if (result.status == 200) {
            cleanSummary = "${cleanSummary}\n${name}:${tag}"
            removalCount++
        }
        return result.status
    }

    String clean() {
        if (dryRun) {
            cleanSummary = "Dry Run Removal:"
        } else {
            cleanSummary = "Images Removed:"
        }
        registryKeys.each { registryKey ->
            cleanRegistry(registryKey)
        }
        cleanSummary = "${cleanSummary}\n\nTotal Images Removed: ${removalCount}"
        return cleanSummary
    }

    protected cleanRegistry(String registryKey) {
        def registry = Config.global.getRegistry(registryKey)

        if (dryRun) {
            Config.pipeline.echo "Dry Run"
        }
        Config.pipeline.echo "RetentionDays: ${retentionDays}"

        def registryRepositories = readRepositories(registry)
        registryRepositories.each { repo, tags ->

            Config.pipeline.echo "Processing repo ${repo}"
            def imageManifests = new ImageManifests(new Image(path: "${repo}"))
            tags.each { tagData ->
                imageManifests.addArtifactoryManifest(tagData)
            }
            imageManifests.getCleanableTagsList(retentionDays).each { tag ->
                deleteTag(registry, repo, tag)
            }

        }

    }

}
