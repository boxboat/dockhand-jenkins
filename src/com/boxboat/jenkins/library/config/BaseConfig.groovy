package com.boxboat.jenkins.library.config

@Grab('org.apache.commons:commons-lang3:3.7')
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
@Grab('org.yaml:snakeyaml:1.19')
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor
import org.yaml.snakeyaml.representer.Representer

import java.lang.reflect.Modifier

abstract class BaseConfig<T> implements Serializable, ICopyableConfig<T>, IMergeableConfig<T> {

    T newFromYaml(String yamlStr) {
        Yaml yaml = new Yaml(new CustomClassLoaderConstructor(this.class.classLoader))
        return (T) yaml.loadAs(yamlStr, this.class)
    }

    T newFromObject(Object obj) {
        Yaml yaml = new Yaml(new GroovyRepresenter())
        return newFromYaml(yaml.dump(obj))
    }

    T newDefault() {
        def newT = this.class.newInstance()
        this.class.metaClass.properties.each { property ->
            def name = property.name
            def type = property.type
            if (name != "class"
                    && !Modifier.isStatic(property.getModifiers())
                    && type != Object
                    && newT."$name" == null) {
                switch (type) {
                    case BaseConfig:
                        newT."$name" = ((BaseConfig) type.newInstance()).newDefault()
                        break
                    case List:
                        newT."$name" = []
                        break
                    case Map:
                        newT."$name" = [:]
                        break
                    default:
                        newT."$name" = type.newInstance()
                        break
                }
            }
        }
        return (T) newT
    }

    @Override
    T copy() {
        Yaml yaml = new Yaml(new GroovyRepresenter())
        return newFromYaml(yaml.dump(this))
    }

    @Override
    void merge(T other) {
        if (other == null) {
            return
        }
        this.class.metaClass.properties.each { property ->
            def name = property.name
            def type = property.type
            def otherProperty = other.class.metaClass.getMetaProperty(name)
            if (otherProperty != null
                    && otherProperty.type == type
                    && name != "class"
                    && !Modifier.isStatic(property.getModifiers())
                    && other."$name" != null) {
                switch (type) {
                    case IMergeableConfig:
                        if (this."$name" == null) {
                            this."$name" = type.newInstance()
                        }
                        this."$name".merge(other."$name")
                        break
                    default:
                        this."$name" = other."$name"
                        break
                }
            }
        }
    }

    @Override
    boolean equals(Object o) {
        if (!(o instanceof T)) {
            return false
        }
        T m = (T) o

        def equalsBuilder = new EqualsBuilder()
        this.properties.each { k, v ->
            equalsBuilder.append(v, m."$k")
        }
        return equalsBuilder.equals
    }

    @Override
    int hashCode() {
        def hashCodeBuilder = new HashCodeBuilder()
        this.properties.each { k, v ->
            hashCodeBuilder.append(v)
        }
        return hashCodeBuilder.toHashCode()
    }

}

// https://stackoverflow.com/a/35108062/1419658
class GroovyRepresenter extends Representer {

    GroovyRepresenter() {
        this.multiRepresenters.put(GString.class, this.representers.get(String))
    }

}
