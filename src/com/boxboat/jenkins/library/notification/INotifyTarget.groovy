package com.boxboat.jenkins.library.notification

interface INotifyTarget {

    void postMessage(String message, NotificationType notificationType)

}
