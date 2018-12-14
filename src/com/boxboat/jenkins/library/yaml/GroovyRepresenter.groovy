package com.boxboat.jenkins.library.yaml

import com.cloudbees.groovy.cps.NonCPS
@Grab('org.yaml:snakeyaml:1.23')
import org.yaml.snakeyaml.representer.Representer
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.introspector.Property
import org.yaml.snakeyaml.nodes.Tag

// https://stackoverflow.com/a/35108062/1419658
class GroovyRepresenter extends Representer implements Serializable {

    GroovyRepresenter() {
        this.multiRepresenters.put(GString.class, this.representers.get(String))
    }

    @NonCPS // required, Jenkins returns a NodeTuple with yaml.dump otherwise
    @Override
    protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
        if (property.getName() == "metaClass") {
            return null
        }
        return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag)
    }

}
