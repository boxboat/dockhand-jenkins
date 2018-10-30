#!/bin/bash

dir="$1"
profile="$2"
registry="$3"

cd "$dir"

set -e
docker-compose -p "${profile}" build --build-arg REGISTRY="${registry}"
