#!/bin/sh

cd $(dirname $0)

docker run --rm \
    -v "gradle-cache:/home/gradle/.gradle" \
    -v "$(pwd):/home/gradle/project" \
    -w "/home/gradle/project" \
    --entrypoint gradle \
    gradle:4.4.1-jdk8-alpine \
        "$@"
