package com.boxboat.jenkins.library.notification

import com.boxboat.jenkins.library.config.BaseConfig
import groovy.json.JsonBuilder

class SlackNotifyTarget extends BaseConfig<SlackNotifyTarget> implements INotifyTarget {

    String credential = ""

    @Override
    void postMessage(steps, String message, NotificationType notificationType) {
        String color = "#858585"
        switch (notificationType) {
            case NotificationType.FAILURE:
                color = "#d50200"
                break
            case NotificationType.SUCCESS:
                color = "#36a64f"
                break
        }
        steps.withCredentials([
                steps.string(credentialsId: credential, variable: 'SLACK_URL',)
        ]) {
            String jsonStr = new JsonBuilder([
                    text: "*${steps.env.JOB_NAME}* (<${steps.env.BUILD_URL}|build #${steps.env.BUILD_NUMBER}>)",
                    attachments: [
                            [
                                    color: color,
                                    text: message,
                            ]
                    ]
            ]).toString()
            steps.sh 'echo "$SLACK_URL" | base64'
            steps.println jsonStr
            steps.httpRequest(
                    url: steps.env.SLACK_URL,
                    httpMode: 'POST',
                    contentType: "APPLICATION_JSON",
                    requestBody: jsonStr
            )
        }
    }

}
