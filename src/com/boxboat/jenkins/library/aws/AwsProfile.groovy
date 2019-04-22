package com.boxboat.jenkins.library.aws

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config

class AwsProfile extends BaseConfig<AwsProfile> implements Serializable {


    String region

    String accessKeyIdCredential

    String secretKeyCredential


    def withCredentials(Closure closure) {
        List<Object> credentials = []
        if (accessKeyIdCredential) {
            credentials.add(Config.pipeline.string(credentialsId: accessKeyIdCredential, variable: 'AWS_ACCESS_KEY',))
        }
        if (secretKeyCredential) {
            credentials.add(Config.pipeline.string(credentialsId: secretKeyCredential, variable: 'AWS_SECRET_KEY',))
        }
        Config.pipeline.withCredentials(credentials) {
            closure()
        }
    }
}