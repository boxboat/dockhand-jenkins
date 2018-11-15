package com.boxboat.jenkins.library.notification

interface INotificationProvider {

    void postMessage(steps, String message, NotificationType notificationType)

}
