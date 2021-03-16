#!/bin/sh -e

cd $(dirname $0)

if ! docker volume inspect gradle-cache >/dev/null 2>&1; then
    docker volume create gradle-cache
fi

docker run --rm -i \
    -v "gradle-cache:/home/gradle/.gradle" \
    -v "$(pwd):/home/gradle/project" \
    -w "/home/gradle/project" \
    --entrypoint gradle \
    gradle:6.8.3-jdk15 \
        "$@"
