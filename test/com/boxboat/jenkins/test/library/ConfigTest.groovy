package com.boxboat.jenkins.test.library

import com.boxboat.jenkins.library.Config
import com.boxboat.jenkins.library.Vault
import com.boxboat.jenkins.library.docker.Registry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import org.yaml.snakeyaml.Yaml

import static org.junit.Assert.assertEquals

@RunWith(value = Parameterized.class)
class ConfigTest {

    final fileBase = 'test-resources/com/boxboat/jenkins/test/library/config/'

    @Parameter(value = 0)
    public String fileName

    @Parameter(value = 1)
    public Config expectedConfig

    @Test
    void testConfig() {
        def config = Config.createConfig(fileText("${fileBase}${fileName}"))
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
                        new Config(
                                buildVersionsGitRemoteUrl: "ssh://git@github.com/boxboat/build-versions.git",
                                gitEmail: "jenkins@boxboat.com",
                                gitCredentials: "git",
                                registryMap: [
                                        "default": new Registry(
                                                scheme: "https",
                                                host: "dtr.boxboat.com",
                                                credentials: "registry",
                                        )
                                ],
                                vaultMap: [
                                        "default": new Vault(
                                                url: "http://localhost:8200",
                                                "credentials": "vault",
                                        )
                                ],
                        )
                ]]*.toArray()
    }

    private static String fileText(String filename) {
        return new File(filename).getText('UTF-8')
    }

}
