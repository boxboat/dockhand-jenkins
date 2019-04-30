package com.boxboat.jenkins.test.library.config

import com.boxboat.jenkins.library.aws.AwsProfile
import com.boxboat.jenkins.library.notify.SlackJenkinsAppNotifyTarget
import com.boxboat.jenkins.library.vault.Vault
import com.boxboat.jenkins.library.config.CommonConfig
import com.boxboat.jenkins.library.config.DeployConfig
import com.boxboat.jenkins.library.config.GlobalConfig
import com.boxboat.jenkins.library.config.PromoteConfig
import com.boxboat.jenkins.library.deploy.Deployment

import com.boxboat.jenkins.library.deployTarget.KubernetesDeployTarget
import com.boxboat.jenkins.library.docker.Registry
import com.boxboat.jenkins.library.environment.BaseEnvironment
import com.boxboat.jenkins.library.environment.Environment
import com.boxboat.jenkins.library.event.EventRegistryKey
import com.boxboat.jenkins.library.git.GitConfig
import com.boxboat.jenkins.library.notify.SlackWebHookNotifyTarget
import com.boxboat.jenkins.library.promote.Promotion
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

import static org.junit.Assert.assertEquals

@RunWith(value = Parameterized.class)
class GlobalConfigTest {

    final fileBase = 'test-resources/com/boxboat/jenkins/test/library/config/globalConfig/'

    @Parameter(value = 0)
    public String fileName

    @Parameter(value = 1)
    public GlobalConfig expectedConfig

    @Test
    void testConfig() {
        def config = new GlobalConfig().newFromYaml(fileText("${fileBase}${fileName}"))
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
        assertEquals(config.git.buildVersionsUrl, config.git.getRemoteUrl(config.git.getRemotePath(config.git.buildVersionsUrl)))
    }

    @Parameters(name = "{index}: {0}")
    static Collection<Object[]> data() {
        return [[
                        "test.yaml",
                        new GlobalConfig(
                                deployTargetMap: [
                                        "dev01" : new KubernetesDeployTarget(
                                                contextName: "boxboat",
                                                credential: "kubeconfig-dev",
                                        ),
                                        "prod01": new KubernetesDeployTarget(
                                                contextName: "boxboat",
                                                credential: "kubeconfig-prod",
                                        ),
                                        "prod02": new KubernetesDeployTarget(
                                                contextName: "boxboat",
                                                credential: "kubeconfig-prod-02",
                                        ),
                                ],
                                environmentMap: [
                                        "dev" : new Environment(
                                                name: "dev",
                                                deployTargetKey: "dev01",
                                        ),
                                        "prod": new Environment(
                                                name: "prod-a",
                                                deployTargetKey: "prod01",
                                                replicaEnvironments: [
                                                     new BaseEnvironment(
                                                         name: "prod-b",
                                                         deployTargetKey: "prod02"
                                                     )
                                                ]
                                        ),
                                ],
                                git: new GitConfig(
                                        buildVersionsUrl: "git@github.com:boxboat/build-versions.git",
                                        credential: "git",
                                        email: "jenkins@boxboat.com",
                                        remotePathRegex: 'github\\.com[:\\/]boxboat\\/(.*)\\.git$',
                                        remoteUrlReplace: 'git@github.com:boxboat/{{ path }}.git',
                                        branchUrlReplace: "https://github.com/boxboat/{{ path }}/tree/{{ branch }}",
                                        commitUrlReplace: "https://github.com/boxboat/{{ path }}/commit/{{ hash }}",
                                ),
                                notifyTargetMap: [
                                        default: new SlackWebHookNotifyTarget(
                                                credential: "slack-webhook-url",
                                        ),
                                ],
                                registryMap: [
                                        "default": new Registry(
                                                scheme: "https",
                                                host: "dtr.boxboat.com",
                                                credential: "registry",
                                                imageUrlReplace: "https://dtr.boxboat.com/repositories/{{ path }}/{{ tag }}/linux/amd64/layers",
                                        ),
                                ],
                                vaultMap: [
                                        "default": new Vault(
                                                kvVersion: 1,
                                                roleIdCredential: "vault-role-id",
                                                secretIdCredential: "vault-secret-id",
                                                tokenCredential: "vault-token",
                                                url: "http://localhost:8200",
                                        ),
                                ],
                                awsProfileMap: [
                                        "default": new AwsProfile(
                                                region: "us-east-1",
                                                accessKeyIdCredential: "aws-access-key-id",
                                                secretAccessKeyCredential: "aws-secret-access-key",
                                        ),
                                ],
                                repo: [
                                        common : new CommonConfig(
                                                defaultBranch: "master",
                                                notify: [
                                                        "targetMap"     : [
                                                                "jenkins": new SlackJenkinsAppNotifyTarget(
                                                                        channel: "#jenkins"
                                                                ),
                                                        ],
                                                        "successKeys"   : [
                                                                "default",
                                                                "jenkins",
                                                        ],
                                                        successTargets  : [
                                                                new SlackJenkinsAppNotifyTarget(
                                                                        channel: "#jenkins-success"
                                                                ),
                                                        ],
                                                        "failureKeys"   : [
                                                                "default",
                                                                "jenkins",
                                                        ],
                                                        "failureTargets": [
                                                                new SlackJenkinsAppNotifyTarget(
                                                                        channel: "#jenkins-failure"
                                                                ),
                                                        ],
                                                        "infoKeys"      : [
                                                                "default",
                                                                "jenkins",
                                                        ],
                                                        "infoTargets"   : [
                                                                new SlackJenkinsAppNotifyTarget(
                                                                        channel: "#jenkins-info"
                                                                ),
                                                        ],
                                                ],
                                                eventRegistryKeys: [
                                                        new EventRegistryKey(
                                                                event: "commit/master",
                                                                registryKey: "default",
                                                        ),
                                                        new EventRegistryKey(
                                                                eventRegex: "tag/(.*)",
                                                                registryKey: "default",
                                                        ),
                                                ],
                                                vaultKey: "default"
                                        ),
                                        promote: new PromoteConfig(
                                                promotionMap: [
                                                        stage: new Promotion(
                                                                event: "commit/master",
                                                                promoteToEvent: "tag/rc",
                                                        ),
                                                        prod : new Promotion(
                                                                event: "tag/rc",
                                                                promoteToEvent: "tag/release",
                                                        ),
                                                ]
                                        ),
                                        deploy : new DeployConfig(
                                                deploymentMap: [
                                                        dev  : new Deployment(
                                                                environmentKey: "dev",
                                                                event: "commit/master",
                                                                trigger: true,
                                                        ),
                                                        stage: new Deployment(
                                                                environmentKey: "dev",
                                                                event: "tag/rc",
                                                                trigger: true,
                                                        ),
                                                        prod : new Deployment(
                                                                environmentKey: "prod",
                                                                event: "tag/release",
                                                                trigger: false,
                                                        ),
                                                ],
                                        ),
                                ]
                        )
                ]]*.toArray()
    }

    private static String fileText(String filename) {
        return new File(filename).getText('UTF-8')
    }

}
