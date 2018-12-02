#!/bin/bash

container=""
namespace=""
output=""
selector=""

usage () {
    echo "Usage: ./pod-logs.sh --output <filename> [--namespace <kube namespace>] [--selector <kube labels>]" >&2
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
        "-o" | "--output")
            output="$2"
            shift
            ;;
        "-l" | "--selector")
            selector="$2"
            shift
            ;;
        *)
            ;;
    esac
    shift
done

set -e
set -o pipefail

if [ "$output" = "" ]; then
    echo "--output <filename> is required" >&2
    usage
    exit 1
fi

kubectl_get_pod () {
    kubectl ${namespace:+"--namespace=$namespace"} get pods \
        ${1:+"--field-selector=$1"} \
        ${selector:+"--selector=$selector"} \
        -o name \
        --watch \
        &
    echo "$!" >> "${output}.get_pod.fifo"
}

get_pod () {
    read pod
    echo "$pod"
    kill $(<"${output}.get_pod.fifo")
}

echo "Pod not found" > "$output"

# find the pod
rm -f "${output}.get_pod.fifo"
mkfifo "${output}.get_pod.fifo"
pod=$(kubectl_get_pod "status.phase=Running" | get_pod)
rm -f "${output}.get_pod.fifo"

echo "pod    : ${pod}" >&2
echo "output : ${output}" >&2
rm -f "$output"
touch "$output"

# write the output
kubectl ${namespace:+"--namespace=$namespace"} logs -f \
    ${container:+"--container=$container"} \
    "$pod" \
    2>&1 \
| while read line; do
      echo "$line" >> "$output"
done
