package com.boxboat.jenkins.library.notify

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config
import com.boxboat.jenkins.library.credentials.vault.VaultStringCredential
import groovy.json.JsonBuilder

class GoogleChatWebHookNotifyTarget extends BaseConfig<GoogleChatWebHookNotifyTarget> implements INotifyTarget, Serializable {

    Object credential

    @Override
    void postMessage(String message, NotifyType notifyType) {
        String status = ""
        switch (notifyType) {
            case NotifyType.FAILURE:
                status = "Failed "
                break
            case NotifyType.SUCCESS:
                status = "Success "
                break
        }

        Closure closure = {
            String jsonStr = new JsonBuilder([
                    text: """*${status} - ${URLDecoder.decode(Config.pipeline.env.JOB_NAME, "UTF-8")}* (<${Config.pipeline.env.BUILD_URL}|build #${Config.pipeline.env.BUILD_NUMBER}>)
${message}""",
            ]).toString()
            Config.pipeline.httpRequest(
                    url: Config.pipeline.env.GOOGLE_CHAT_URL,
                    httpMode: 'POST',
                    contentType: "APPLICATION_JSON",
                    requestBody: jsonStr
            )
        }

        if (credential instanceof VaultStringCredential) {
            credential.withCredentials(['variable': 'GOOGLE_CHAT_URL']) {
                closure()
            }
        } else {
            Config.pipeline.withCredentials([
                    Config.pipeline.string(credentialsId: credential, variable: 'GOOGLE_CHAT_URL',)
            ]) {
                closure()
            }
        }
    }

}
