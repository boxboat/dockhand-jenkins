package com.boxboat.jenkins.library.git

import java.time.Instant

/**
 * Stores git commit data scraped from git log
 */
class GitCommit implements Serializable {
    String author
    String hash
    String date
    String subject
    String body

    Instant getInstant() {
        return Instant.ofEpochSecond(Long.parseLong(date))
    }

    String toString() {
        return new String("{\"author\":\"${author}\",\"hash\":\"${hash}\",\"date\":\"${getInstant().toString()}\",\"subject\":\"${subject}\",\"body\":\"${body}\"}")
    }

}
