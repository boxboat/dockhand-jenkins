package com.boxboat.jenkins.library.deployTarget

interface IDeployTarget {

    void withCredentials(steps, closure)

}
