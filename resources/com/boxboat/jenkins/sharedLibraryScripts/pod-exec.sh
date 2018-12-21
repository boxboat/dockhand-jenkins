#!/bin/bash

container=""
namespace=""
output="-"
selector=""
command=()

usage () {
    echo "Usage: ./pod-exec.sh [--namespace <kube namespace>] [--selector <kube labels>]" command... >&2
}

while [ $# -gt 0 ]; do
    case "$1" in
        "-c" | "--container")
            container="$2"
            shift
            ;;
        "-n" | "--namespace")
            namespace="$2"
            shift
            ;;
        "-l" | "--selector")
            selector="$2"
            shift
            ;;
        *)
            command+=("$1")
            ;;
    esac
    shift
done

set -e

pod=$(kubectl ${namespace:+"--namespace=$namespace"} get pods \
    "--field-selector=status.phase=Running" \
    ${selector:+"--selector=$selector"} \
    --no-headers \
    -o "custom-columns=:metadata.name" \
    | head -n 1)

if [ -z "$pod" ]; then
    echo "No running pod in namespace '${namespace}' matching selector '${selector}' was found" >&2
    exit 1
fi

echo "pod    : ${pod}" >&2


kubectl ${namespace:+"--namespace=$namespace"} exec \
    ${container:+"--container=$container"} \
    "$pod" \
    -- \
    "${command[@]}"
