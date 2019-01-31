package com.boxboat.jenkins.library.notify

import com.boxboat.jenkins.library.config.BaseConfig
import com.boxboat.jenkins.library.config.Config

class NotifyConfig extends BaseConfig implements Serializable {

    Map<String, INotifyTarget> targetMap

    List<String> successKeys

    List<INotifyTarget> successTargets

    List<String> failureKeys

    List<INotifyTarget> failureTargets

    List<String> infoKeys

    List<INotifyTarget> infoTargets

    INotifyTarget getNotifyTarget(String key) {
        def target = targetMap.get(key)
        if (!target) {
            target = Config.global.notifyTargetMap.get(key)
        }
        if (!target) {
            throw new Exception("notify target entry '${key}' does not exist in repository notify.targetMap or global notifyTargetMap")
        }
        return target
    }

    List<INotifyTarget> successTargets() {
        return successTargets + successKeys.collect { key -> return getNotifyTarget(key) }
    }

    List<INotifyTarget> failureTargets() {
        return failureTargets + failureKeys.collect { key -> return getNotifyTarget(key) }
    }

    List<INotifyTarget> infoTargets() {
        return infoTargets + infoKeys.collect { key -> return getNotifyTarget(key) }
    }

}
