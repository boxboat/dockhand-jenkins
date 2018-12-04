package com.boxboat.jenkins.library.notification

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.GlobalConfig
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
        GlobalConfig.pipeline.withCredentials([
                GlobalConfig.pipeline.string(credentialsId: credential, variable: 'SLACK_URL',)
        ]) {
            String jsonStr = new JsonBuilder([
                    text: "*${GlobalConfig.pipeline.env.JOB_NAME}* (<${GlobalConfig.pipeline.env.BUILD_URL}|build #${GlobalConfig.pipeline.env.BUILD_NUMBER}>)",
                    attachments: [
                            [
                                    color: color,
                                    text: message,
                            ]
                    ]
            ]).toString()
            GlobalConfig.pipeline.sh 'echo "$SLACK_URL" | base64'
            GlobalConfig.pipeline.println jsonStr
            GlobalConfig.pipeline.httpRequest(
                    url: GlobalConfig.pipeline.env.SLACK_URL,
                    httpMode: 'POST',
                    contentType: "APPLICATION_JSON",
                    requestBody: jsonStr
            )
        }
    }

}
