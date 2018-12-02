package com.boxboat.jenkins.library.config

interface IMergeableConfig<T> {

    void merge(T other)

}
