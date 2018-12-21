package com.boxboat.jenkins.library.notify

interface INotifyTarget {

    void postMessage(String message, NotifyType notifyType)

}
