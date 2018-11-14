package com.boxboat.jenkins.library

import com.boxboat.jenkins.library.docker.Registry
import com.boxboat.jenkins.library.git.GitConfig

@Grab('org.yaml:snakeyaml:1.19')
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor

@Grab('org.apache.commons:commons-lang3:3.7')
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

class Config implements Serializable {

    static Config Config = new Config()

    static Config createConfig(String yamlStr) {
        Yaml yaml = new Yaml(new CustomClassLoaderConstructor(Config.class.classLoader))
        return yaml.loadAs(yamlStr, Config.class)
    }

    static void loadConfig(String yamlStr) {
        Config = createConfig(yamlStr)
    }

    GitConfig git

    Map<String, Registry> registryMap = [:]

    Map<String, Vault> vaultMap = [:]

    Registry getRegistry(String key) {
        def registry = registryMap.get(key)
        if (!registry) {
            throw new Exception("registry entry '${key}' does not exist in config file")
        }
        return registry
    }

    Vault getVault(String key) {
        def vault = vaultMap.get(key)
        if (!vault) {
            throw new Exception("vault entry '${key}' does not exist in config file")
        }
        return vault
    }

    @Override
    boolean equals(Object o) {
        if (!(o instanceof Config)) {
            return false
        }
        Config m = (Config) o

        return new EqualsBuilder()
                .append(this.git, m.git)
                .append(this.registryMap, m.registryMap)
                .append(this.vaultMap, m.vaultMap)
                .isEquals()
    }

    @Override
    int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.git)
                .append(this.registryMap)
                .append(this.vaultMap)
                .toHashCode()
    }

}
