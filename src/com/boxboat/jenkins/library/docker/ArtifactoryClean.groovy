package com.boxboat.jenkins.library.docker

import com.boxboat.jenkins.library.config.Config

class ArtifactoryClean implements Serializable {

    boolean dryRun
    int retentionDays
    int removalCount = 0
    List<ArtifactoryCleanRegistry> artifactoryCleanRegistries
    String cleanSummary

    Map<String, List<ArtifactoryImageManifest>> readRepositories(ArtifactoryCleanRegistry cleanRegistry) {
        def registry = Config.global.getRegistry(cleanRegistry.registryKey)
        def requestURI = registry.getRegistryUrl() + "/artifactory/api/search/aql"
        def pathMatch = cleanRegistry.pathMatch ?: "*"

        def findString = """items.find({"path":{"\$ne":"."},"\$or":[{"\$and":[{"repo":"${registry.namespace}","path":{"\$match":"${pathMatch}"},"name":"manifest.json"}]}]}).include("name","repo","path","actual_md5","actual_sha1","size","type","modified","created","property")"""
        Config.pipeline.echo "${requestURI}"
        Config.pipeline.echo "${findString}"
                def result = Config.pipeline.httpRequest(
                url: requestURI,
                httpMode: 'POST',
                contentType: "TEXT_PLAIN",
                authentication: registry.credential,
                requestBody: findString
        )?.getContent()
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

    int deleteTag(ArtifactoryCleanRegistry cleanRegistry, String name, String tag) {
        def registry = Config.global.getRegistry(cleanRegistry.registryKey)
        def result
        Config.pipeline.echo "Removing ${name}:${tag}"
        if (dryRun) {
            cleanSummary = "${cleanSummary}\n${name}:${tag}"
            removalCount++
            return 200
        }

        // clean up tags
        def requestURI = registry.getRegistryUrl() + "/artifactory/${registry.namespace}/${name}/${tag}"
        result = Config.pipeline.httpRequest(
                url: requestURI,
                httpMode: 'DELETE',
                contentType: "APPLICATION_JSON",
                authentication: registry.credential,
        )?.getStatus()
        if (result == 200 || result == 204) {
            cleanSummary = "${cleanSummary}\n${name}:${tag}"
            removalCount++
        }
        return result
    }

    String clean() {
        if (dryRun) {
            cleanSummary = "Dry Run Removal:"
        } else {
            cleanSummary = "Images Removed:"
        }
        artifactoryCleanRegistries.each { registry ->
            cleanRegistry(registry)
        }
        cleanSummary = "${cleanSummary}\n\nTotal Images Removed: ${removalCount}"
        return cleanSummary
    }

    protected cleanRegistry(ArtifactoryCleanRegistry cleanRegistry) {
        if (dryRun) {
            Config.pipeline.echo "Dry Run"
        }
        Config.pipeline.echo "RetentionDays: ${retentionDays}"
        def registryRepositories = readRepositories(cleanRegistry)
        registryRepositories.each { repo, tags ->

            Config.pipeline.echo "Processing repo ${repo}"
            def imageManifests = new ImageManifests(new Image(path: "${repo}"))
            tags.each { tagData ->
                imageManifests.addArtifactoryManifest(tagData)
            }
            imageManifests.getCleanableTagsList(retentionDays).each { tag ->
                deleteTag(cleanRegistry, repo, tag)
            }
        }

    }

}
