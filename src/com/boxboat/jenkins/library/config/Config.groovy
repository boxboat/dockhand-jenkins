package com.boxboat.jenkins.library.config

class Config implements Serializable {

    static String baseDir = "./"

    static String scmDir = "./"

    static GlobalConfig global

    static CommonConfigBase repo

    static Object pipeline

    static <T extends CommonConfigBase> T castRepo() {
        return (T) repo
    }

}
