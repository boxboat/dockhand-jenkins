package com.boxboat.jenkins.library.config

import com.boxboat.jenkins.library.yaml.YamlUtils

@Grab('org.apache.commons:commons-lang3:3.9')
import org.apache.commons.lang.builder.ToStringBuilder
import org.apache.commons.lang.builder.ToStringStyle
import org.apache.commons.lang3.builder.EqualsBuilder

import java.lang.reflect.Modifier

abstract class BaseConfig<T> implements Serializable, ICopyableConfig<T>, IMergeableConfig<T> {

    T newFromYaml(String yamlStr) {
        return (T) YamlUtils.loadAs(yamlStr, this.class)
    }

    T newFromObject(Object obj) {
        return newFromYaml(YamlUtils.dump(obj))
    }

    T newDefault() {
        def newT = this.class.newInstance()
        this.class.metaClass.properties.each { property ->
            def name = property.name
            def type = property.type
            if (name != "class"
                    && !Modifier.isStatic(property.getModifiers())
                    && !(this.respondsTo("get${name.capitalize()}") && !this.respondsTo("set${name.capitalize()}"))
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
                        newT."$name" = null
                        break
                }
            }
        }
        return (T) newT
    }

    public Map asMap() {
        this.class.declaredFields.findAll { !it.synthetic }.collectEntries {
            [(it.name): this."$it.name"]
        }
    }

    @Override
    T copy() {
        return newFromYaml(this.dumpYaml())
    }

    String dumpYaml() {
        return YamlUtils.dump(this)
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
                    && !(this.respondsTo("get${name.capitalize()}") && !this.respondsTo("set${name.capitalize()}"))
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
        this.class.metaClass.properties.each { property ->
            def name = property.name
            if (name == "class"
                    || Modifier.isStatic(property.getModifiers())
                    || (this.respondsTo("get${name.capitalize()}") && !this.respondsTo("set${name.capitalize()}"))) {
                return
            }
            equalsBuilder.append(this."${name}", m."${name}")
        }
        return equalsBuilder.equals
    }

    @Override
    String toString() {
        def toStringBuilder = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
        this.class.metaClass.properties.each { property ->
            def name = property.name
            if (name == "class"
                    || Modifier.isStatic(property.getModifiers())
                    || (this.respondsTo("get${name.capitalize()}") && !this.respondsTo("set${name.capitalize()}"))) {
                return
            }
            toStringBuilder.append(name, this."${name}")
        }
        return toStringBuilder.toString()
    }

}
