package com.boxboat.jenkins.library.notify

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config

class SlackJenkinsAppNotifyTarget extends BaseConfig<SlackJenkinsAppNotifyTarget> implements INotifyTarget, Serializable {

    String channel

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
        message = "*${Config.pipeline.env.JOB_NAME}* (<${Config.pipeline.env.BUILD_URL}|build #${Config.pipeline.env.BUILD_NUMBER}>)\n" + message
        def slackSendOptions = [
                color  : color,
                message: message,
        ]
        if (channel) {
            slackSendOptions["channel"] = channel
        }
        Config.pipeline.slackSend(slackSendOptions)
    }

}
