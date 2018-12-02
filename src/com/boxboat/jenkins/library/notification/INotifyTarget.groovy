package com.boxboat.jenkins.library.notification

interface INotifyTarget {

    void postMessage(steps, String message, NotificationType notificationType)

}
