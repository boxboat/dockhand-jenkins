package com.boxboat.jenkins.library

@Grab('org.apache.commons:commons-lang3:3.7')
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

class Vault implements Serializable {

    int kvVersion

    String roleIdCredential = ""

    String secretIdCredential = ""

    String tokenCredential = ""

    String url = ""

    List<Object> getCredentials(steps) {
        List<Object> credentials = []
        if (roleIdCredential){
            credentials.add(steps.string(credentialsId: roleIdCredential, variable: 'VAULT_ROLE_ID',))
        }
        if (secretIdCredential){
            credentials.add(steps.string(credentialsId: secretIdCredential, variable: 'VAULT_SECRET_ID',))
        }
        if (tokenCredential){
            credentials.add(steps.string(credentialsId: tokenCredential, variable: 'VAULT_TOKEN',))
        }
        return credentials
    }

    @Override
    boolean equals(Object o) {
        if (!(o instanceof Vault)) {
            return false
        }
        Vault m = (Vault) o

        return new EqualsBuilder()
                .append(this.kvVersion, m.kvVersion)
                .append(this.roleIdCredential, m.roleIdCredential)
                .append(this.secretIdCredential, m.secretIdCredential)
                .append(this.tokenCredential, m.tokenCredential)
                .append(this.url, m.url)
                .isEquals()
    }

    @Override
    int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.kvVersion)
                .append(this.roleIdCredential)
                .append(this.secretIdCredential)
                .append(this.tokenCredential)
                .append(this.url)
                .toHashCode()
    }

}
