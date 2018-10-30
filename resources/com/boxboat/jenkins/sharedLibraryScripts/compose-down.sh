#!/bin/bash

dir="$1"
profile="$2"

cd "$dir"

docker-compose -p "${profile}" down
exit 0
