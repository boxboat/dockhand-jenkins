package com.boxboat.jenkins.library.credentials

interface ICredential {
    def withCredentials(Closure closure)
}
