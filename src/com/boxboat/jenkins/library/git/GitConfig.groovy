package com.boxboat.jenkins.library.git

@Grab('org.apache.commons:commons-lang3:3.7')
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

class GitConfig {

    String buildVersionsUrl = ""

    String email = ""

    String credential = ""

    String remotePathRegex = ""

    String remoteUrlReplace = ""

    String getRemotePath(String url) {
        def matcher = url =~ remotePathRegex
        return matcher.hasGroup() ? matcher[0][1] : null
    }

    String getRemoteUrl(String path) {
        return remoteUrlReplace.replaceFirst(/(?i)\{\{\s+path\s+\}\}/, path)
    }

    @Override
    boolean equals(Object o) {
        if (!(o instanceof GitConfig)) {
            return false
        }
        GitConfig m = (GitConfig) o

        return new EqualsBuilder()
                .append(this.buildVersionsUrl, m.buildVersionsUrl)
                .append(this.email, m.email)
                .append(this.credential, m.credential)
                .append(this.remotePathRegex, m.remotePathRegex)
                .append(this.remoteUrlReplace, m.remoteUrlReplace)
                .isEquals()
    }

    @Override
    int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.buildVersionsUrl)
                .append(this.email)
                .append(this.credential)
                .append(this.remotePathRegex)
                .append(this.remoteUrlReplace)
                .toHashCode()
    }


}
