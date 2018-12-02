#!/bin/bash

dir="$1"
profile="$2"

cd "$dir"

set -e
docker-compose -p "${profile}" build
