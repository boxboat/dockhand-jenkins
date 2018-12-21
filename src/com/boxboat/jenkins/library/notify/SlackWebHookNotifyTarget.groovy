package com.boxboat.jenkins.library.notify

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config
import groovy.json.JsonBuilder

class SlackWebHookNotifyTarget extends BaseConfig<SlackWebHookNotifyTarget> implements INotifyTarget, Serializable {

    String credential

    @Override
    void postMessage(String message, NotifyType notifyType) {
        String color = "#858585"
        switch (notifyType) {
            case NotifyType.FAILURE:
                color = "#d50200"
                break
            case NotifyType.SUCCESS:
                color = "#36a64f"
                break
        }
        Config.pipeline.withCredentials([
                Config.pipeline.string(credentialsId: credential, variable: 'SLACK_URL',)
        ]) {
            String jsonStr = new JsonBuilder([
                    text: "*${Config.pipeline.env.JOB_NAME}* (<${Config.pipeline.env.BUILD_URL}|build #${Config.pipeline.env.BUILD_NUMBER}>)",
                    attachments: [
                            [
                                    color: color,
                                    text: message,
                            ]
                    ]
            ]).toString()
            Config.pipeline.httpRequest(
                    url: Config.pipeline.env.SLACK_URL,
                    httpMode: 'POST',
                    contentType: "APPLICATION_JSON",
                    requestBody: jsonStr
            )
        }
    }

}
