#!/bin/bash

for i in "$@"
do
    if ! docker volume inspect nuget-config >/dev/null 2>&1; then
        docker volume create "$i"
    fi
done
