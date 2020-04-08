package com.boxboat.jenkins.test.library.config


import com.boxboat.jenkins.library.config.*
import com.boxboat.jenkins.library.deploy.Deployment
import com.boxboat.jenkins.library.docker.Image
import com.boxboat.jenkins.library.event.EventRegistryKey
import com.boxboat.jenkins.library.promote.Promotion
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

import static org.junit.Assert.assertEquals

@RunWith(value = Parameterized.class)
class RepoConfigTest {

    final fileBase = 'test-resources/com/boxboat/jenkins/test/library/config/repoConfig/'

    @Parameter(value = 0)
    public String fileName

    @Parameter(value = 1)
    public RepoConfig expectedConfig

    @Test
    void testConfig() {
        def config = new RepoConfig().newFromYaml(fileText("${fileBase}${fileName}"))
//        Yaml yaml = new Yaml()
//        System.out.println("")
//        System.out.println("Expected:")
//        System.out.println("")
//        System.out.println(yaml.dump(expectedConfig))
//        System.out.println("")
//        System.out.println("")
//        System.out.println("Actual:")
//        System.out.println("")
//        System.out.println(yaml.dump(config))
        assertEquals(expectedConfig, config)
    }

    @Parameters(name = "{index}: {0}")
    static Collection<Object[]> data() {
        return [[
                        "test.yaml",
                        new RepoConfig(
                                common: new CommonConfig(
                                        images: [
                                                new Image("apps/app-1"),
                                                new Image("apps/app-2"),
                                                new Image("apps/app-3"),
                                                new Image("apps/app-4"),
                                        ],
                                        eventRegistryKeys: [
                                                new EventRegistryKey(
                                                        event: "commit/dev",
                                                        registryKey: "default",
                                                ),
                                                new EventRegistryKey(
                                                        event: "commit/master",
                                                        registryKey: "default",
                                                ),
                                                new EventRegistryKey(
                                                        event: "commit/feature-.*",
                                                        registryKey: "default",
                                                ),
                                                new EventRegistryKey(
                                                        event: "tag/.*",
                                                        registryKey: "default",
                                                ),
                                        ],
                                        "gitAlternateKey": "gitlab",
                                        userConfigMap: [
                                                test: new ArrayList<String>(
                                                        Arrays.asList("foo", "bar")
                                                ),
                                        ],
                                ),
                                build: new BuildConfig(
                                        composeProfileMap: [
                                                "docker": "./build/docker",
                                        ],
                                        pullImages: [
                                                new Image(
                                                        path: "base-images/app-base",
                                                        event: "tag/release",
                                                        trigger: true,
                                                ),
                                        ],
                                ),
                                promote: new PromoteConfig(
                                        baseVersion: "0.0.1",
                                        promotionMap: [
                                                qa   : new Promotion(
                                                        event: "commit/master",
                                                        promoteToEvent: "tag/preview",
                                                ),
                                                stage: new Promotion(
                                                        event: "tag/preview",
                                                        promoteToEvent: "tag/rc",
                                                ),
                                                prod : new Promotion(
                                                        event: "tag/rc",
                                                        promoteToEvent: "tag/release",
                                                ),
                                        ]
                                ),
                                deploy: new DeployConfig(
                                        deploymentMap: [
                                                feature: new Deployment(
                                                        environmentKey: "dev",
                                                        event: "commit/feature-.*",
                                                        eventFallback: "commit/master",
                                                        imageOverrides: [
                                                                new Image(
                                                                        path: "apps/app-4",
                                                                        event: "commit/test",
                                                                        eventFallback: "commit/test2",
                                                                )
                                                        ],
                                                        trigger: true,
                                                        triggerBranch: "feature",
                                                )
                                        ],
                                        imageOverrides: [
                                                new Image(
                                                        path: "apps/app-3",
                                                        event: "commit/test",
                                                        eventFallback: "commit/test2",
                                                )
                                        ]
                                ),
                        )
                ]]*.toArray()
    }

    private static String fileText(String filename) {
        return new File(filename).getText('UTF-8')
    }

}
