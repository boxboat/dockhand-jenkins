package com.boxboat.jenkins.library.deployTarget

@Grab('org.apache.commons:commons-lang3:3.7')
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

class KubernetesDeployTarget implements IDeployTarget {

    String caCertificate

    String contextName

    String credential

    String serverUrl

    @Override
    void withCredentials(steps, closure) {
        steps.withKubeConfig(
                credentialsId: credential,
                caCertificate: caCertificate,
                serverUrl: serverUrl,
                contextName: contextName
        ) {
            closure()
        }
    }

    @Override
    boolean equals(Object o) {
        if (!(o instanceof KubernetesDeployTarget)) {
            return false
        }
        KubernetesDeployTarget m = (KubernetesDeployTarget) o

        return new EqualsBuilder()
                .append(this.credential, m.credential)
                .append(this.caCertificate, m.caCertificate)
                .append(this.serverUrl, m.serverUrl)
                .append(this.contextName, m.contextName)
                .isEquals()
    }

    @Override
    int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.credential)
                .append(this.caCertificate)
                .append(this.serverUrl)
                .append(this.contextName)
                .toHashCode()
    }

}
