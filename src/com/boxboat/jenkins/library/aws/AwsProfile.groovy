package com.boxboat.jenkins.library.aws

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config

class AwsProfile extends BaseConfig<AwsProfile> implements Serializable {


    String region

    String accessKeyIdCredential

    String secretAccessKeyCredential


    def withCredentials(Closure closure) {
        List<Object> credentials = []
        if (accessKeyIdCredential) {
            credentials.add(Config.pipeline.string(credentialsId: accessKeyIdCredential, variable: 'AWS_ACCESS_KEY_ID',))
        }
        if (secretAccessKeyCredential) {
            credentials.add(Config.pipeline.string(credentialsId: secretAccessKeyCredential, variable: 'AWS_SECRET_ACCESS_KEY',))
        }
        Config.pipeline.withCredentials(credentials) {
            closure()
        }
    }
}