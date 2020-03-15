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

docker-compose "${fileArgs[@]}" -p "$profile" down
exit 0
