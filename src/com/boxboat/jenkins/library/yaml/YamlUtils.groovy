package com.boxboat.jenkins.library.yaml

@Grab('org.yaml:snakeyaml:1.23')
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor
import org.yaml.snakeyaml.nodes.Tag

class YamlUtils implements Serializable {

    protected static Yaml yaml = new Yaml(new GroovyRepresenter())

    protected static Yaml yamlClassLoader = new Yaml(new CustomClassLoaderConstructor(YamlUtils.classLoader))

    static String dump(Object obj) {
        return yaml.dump(obj)
    }

    static String hideClassesAndDump(List<Class> classes, obj) {
        def representer = new GroovyRepresenter()
        classes.each { clazz ->
            representer.addClassTag(clazz, Tag.MAP)
        }
        def yaml = new Yaml(representer)
        return yaml.dump(obj)
    }

    static load(String yamlStr) {
        return yaml.load(yamlStr)
    }

    static loadAs(String yamlStr, Class clazz) {
        return yamlClassLoader.loadAs(yamlStr, clazz)
    }

}
