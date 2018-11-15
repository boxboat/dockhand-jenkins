package com.boxboat.jenkins.library.notification

@Grab('org.apache.commons:commons-lang3:3.7')
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

class NotificationsConfig {

    String failureProvider = ""

    Map<String, INotificationProvider> providerMap = [:]

    String successProvider = ""

    INotificationProvider getProvider(String key) {
        def provider = providerMap.get(key)
        if (!provider) {
            throw new Exception("notifications.provierMap entry '${key}' does not exist in config file")
        }
        return provider
    }

    @Override
    boolean equals(Object o) {
        if (!(o instanceof NotificationsConfig)) {
            return false
        }
        NotificationsConfig m = (NotificationsConfig) o

        return new EqualsBuilder()
                .append(this.failureProvider, m.failureProvider)
                .append(this.providerMap, m.providerMap)
                .append(this.successProvider, m.successProvider)
                .isEquals()
    }

    @Override
    int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.failureProvider)
                .append(this.providerMap)
                .append(this.successProvider)
                .toHashCode()
    }

}
