package com.boxboat.jenkins.library.docker

@Grab('org.apache.commons:commons-lang3:3.7')
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

class Registry implements Serializable {

    String scheme = "https"

    String host = ""

    String credential = ""

    def getRegistryUrl() {
        return "${scheme}://${host}"
    }

    @Override
    boolean equals(Object o) {
        if (!(o instanceof Registry)) {
            return false
        }
        Registry m = (Registry) o

        return new EqualsBuilder()
                .append(this.scheme, m.scheme)
                .append(this.host, m.host)
                .append(this.credential, m.credential)
                .isEquals()
    }

    @Override
    int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.scheme)
                .append(this.host)
                .append(this.credential)
                .toHashCode()
    }

}
