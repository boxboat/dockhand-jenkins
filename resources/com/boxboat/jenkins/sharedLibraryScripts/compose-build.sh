#!/bin/bash

path="$1"
profile="$2"

dir="$path"
fileArgs=()
if [ -f "$path" ]; then
    dir=$(dirname $path)
    base=$(basename $path)
    fileArgs=("-f" "$base")
fi
cd "$dir"

set -e
docker-compose "${fileArgs[@]}" -p "$profile" build
