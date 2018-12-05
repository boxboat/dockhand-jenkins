package com.boxboat.jenkins.library.notification

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config
import groovy.json.JsonBuilder

class SlackNotifyTarget extends BaseConfig<SlackNotifyTarget> implements INotifyTarget {

    String credential = ""

    @Override
    void postMessage(String message, NotificationType notificationType) {
        String color = "#858585"
        switch (notificationType) {
            case NotificationType.FAILURE:
                color = "#d50200"
                break
            case NotificationType.SUCCESS:
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
            Config.pipeline.sh 'echo "$SLACK_URL" | base64'
            Config.pipeline.println jsonStr
            Config.pipeline.httpRequest(
                    url: Config.pipeline.env.SLACK_URL,
                    httpMode: 'POST',
                    contentType: "APPLICATION_JSON",
                    requestBody: jsonStr
            )
        }
    }

}
