package com.boxboat.jenkins.library.notification

import groovy.json.JsonBuilder
@Grab('org.apache.commons:commons-lang3:3.7')
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

class SlackNotificationProvider implements INotificationProvider, Serializable {

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

    @Override
    boolean equals(Object o) {
        if (!(o instanceof SlackNotificationProvider)) {
            return false
        }
        SlackNotificationProvider m = (SlackNotificationProvider) o

        return new EqualsBuilder()
                .append(this.credential, m.credential)
                .isEquals()
    }

    @Override
    int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.credential)
                .toHashCode()
    }

}
