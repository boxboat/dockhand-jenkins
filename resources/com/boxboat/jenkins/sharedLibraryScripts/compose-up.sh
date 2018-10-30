#!/bin/bash

dir="$1"
profile="$2"
registry="$3"

cd "$dir"
set -e
docker-compose -p "${profile}" build --build-arg REGISTRY="${registry}"
docker-compose -p "${profile}" up -d
set +e

docker-compose -p "$profile" logs -f &
pid=$!

for i in `seq 1 600`; do
    state="running"
    container_status=$(docker-compose -p "$profile" ps -q | xargs docker inspect --format '{{ .State.Status }}')
    container_health=$(docker-compose -p "$profile" ps -q | xargs docker inspect --format '{{ if .State.Health }}{{ .State.Health.Status }}{{ end }}')
    for status in $container_status; do
        if [ "$status" = "created" ]; then
            state="starting"
        elif [ "$status" != "running" ]; then
            kill $pid
            wait $pid
            docker ps -a >&2
            echo "$profile docker-compose container stopped" >&2
            exit 1
        fi
    done
    for health in $container_health; do
        if [ "$health" = "starting" ]; then
            state="starting"
        elif [ "$health" != "healthy" ]; then
            kill $pid
            wait $pid
            docker ps >&2
            echo "$profile docker-compose health check unhealthy" >&2
            exit 1
        fi
    done
    # exit if successful
    if [ "$state" = "running" ]; then
        kill $pid
        wait $pid
        echo "$profile docker-compose started"
        exit 0
    fi
done

# init script did not run
kill $pid
wait $pid
echo "$profile docker-compose timeout" >&2
exit 1
