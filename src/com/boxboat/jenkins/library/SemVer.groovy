package com.boxboat.jenkins.library

import com.cloudbees.groovy.cps.NonCPS

class SemVer implements Comparable<SemVer>, Serializable {

    boolean isValid

    int major

    int minor

    int patch

    boolean isPreRelease

    String preRelease

    boolean hasBuildInfo

    String buildInfo

    String previousVersion

    SemVer(String version, String previousVersion = "") {
        this.isPreRelease = false
        this.hasBuildInfo = false
        this.previousVersion = previousVersion

        // regex from https://github.com/semver/semver/issues/232
        def regex = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(-(0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(\.(0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*)?(\+[0-9a-zA-Z-]+(\.[0-9a-zA-Z-]+)*)?$/

        def match = version =~ regex
        if (match.matches()) {
            this.isValid = true
            for (def i = 1; i < match.groupCount(); i++) {
                switch (i) {
                    case 1:
                        this.major = match.group(i).toInteger()
                        break
                    case 2:
                        this.minor = match.group(i).toInteger()
                        break
                    case 3:
                        this.patch = match.group(i).toInteger()
                        break
                    default:
                        if (match.group(i)?.startsWith("-")) {
                            this.isPreRelease = true
                            this.preRelease = match.group(i).substring(1)
                        }
                        if (match.group(i)?.startsWith("+")) {
                            this.hasBuildInfo = true
                            this.buildInfo = match.group(i).substring(1)
                        }
                        break
                }
            }
        } else {
            this.isValid = false
        }

    }

    String toString() {
        return """${major}.${minor}.${patch}${preRelease ? "-" + preRelease : ""}${buildInfo ? "+" + buildInfo : ""}"""
    }

    def incrementPreRelease(String preReleaseType) {
        def preReleaseNumber = 0
        if (preRelease && preRelease.startsWith(preReleaseType)) {
            def preReleaseNumberStr = preRelease.substring(preReleaseType.length())
            try {
                preReleaseNumber = preReleaseNumberStr.toInteger()
            } catch (ignored) {
            }
        }
        preReleaseNumber++
        preRelease = "${preReleaseType}${preReleaseNumber}"
        isPreRelease = true
    }

    SemVer copy() {
        return new SemVer(this.toString(), this.getPreviousVersion())
    }

    SemVer copyNoPrerelease() {
        def noPrerelease = copy()
        noPrerelease.preRelease = null
        noPrerelease.isPreRelease = false
        return noPrerelease
    }

    @Override
    @NonCPS
    int compareTo(SemVer semver) {
        if (this.isValid && semver.isValid) {
            if (this.major == semver.major && this.minor == semver.minor && this.patch == semver.patch) {
                if (this.isPreRelease && !semver.isPreRelease) {
                    return -1
                } else if (!this.isPreRelease && semver.isPreRelease) {
                    return 1
                }
                return 0
            }
            if (this.major > semver.major) {
                return 1
            } else if (this.major < semver.major) {
                return -1
            }
            if (this.minor > semver.minor) {
                return 1
            } else if (this.minor < semver.minor) {
                return -1
            }
            if (this.patch > semver.patch) {
                return 1
            } else if (this.patch < semver.patch) {
                return -1
            }
        } else if (this.isValid) {
            return 1
        } else if (semver.isValid) {
            return -1
        }
        return 0
    }

}

