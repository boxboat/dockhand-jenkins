package com.boxboat.jenkins.library.config

import com.boxboat.jenkins.library.promote.Promotion

class PromoteConfig extends CommonConfigBase<PromoteConfig> implements Serializable {

    String baseVersion

    String promotionKey

    Map<String, Promotion> promotionMap

    Promotion getPromotion(String key) {
        def promotion = promotionMap.get(key)
        if (!promotion) {
            throw new Exception("promotion entry '${key}' does not exist in config file")
        }
        return promotion
    }

}
