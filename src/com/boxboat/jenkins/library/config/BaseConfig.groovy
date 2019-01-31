package com.boxboat.jenkins.library.config

import com.boxboat.jenkins.library.yaml.YamlUtils
@Grab('org.apache.commons:commons-lang3:3.8.1')
import org.apache.commons.lang3.builder.EqualsBuilder

import java.lang.reflect.Modifier

abstract class BaseConfig implements Serializable, ICopyableConfig, IMergeableConfig {

    def newFromYaml(String yamlStr) {
        return YamlUtils.loadAs(yamlStr, this.class)
    }

    def newFromObject(Object obj) {
        return newFromYaml(YamlUtils.dump(obj))
    }

    def newDefault() {
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
                        newT."$name" = null
                        break
                }
            }
        }
        return newT
    }

    @Override
    def copy() {
        return newFromYaml(this.dumpYaml())
    }

    String dumpYaml() {
        return YamlUtils.dump(this)
    }

    @Override
    void merge(other) {
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
        if (o.class != this.class) {
            return false
        }

        def equalsBuilder = new EqualsBuilder()
        this.properties.keySet().toList().each { k ->
            def v = this.properties[k]
            if (k == "class") {
                return
            }
            equalsBuilder.append(v, o."$k")
        }
        return equalsBuilder.equals
    }

}
