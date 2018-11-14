package com.boxboat.jenkins.library

@Grab('org.apache.commons:commons-lang3:3.7')
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

class Vault implements Serializable {

    String url = ""

    String credentials = "s"

    @Override
    boolean equals(Object o) {
        if (!(o instanceof Vault)) {
            return false
        }
        Vault m = (Vault) o

        return new EqualsBuilder()
                .append(this.url, m.url)
                .append(this.credentials, m.credentials)
                .isEquals()
    }

    @Override
    int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.url)
                .append(this.credentials)
                .toHashCode()
    }

}
