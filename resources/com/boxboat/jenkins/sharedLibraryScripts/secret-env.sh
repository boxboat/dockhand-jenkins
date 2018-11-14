#!/bin/bash

append=""
data_path=""
format="env"
help=0
keys=()
kv_version=""
sep="="
output=""

usage () {
    echo "Usage: ./secret-env.sh --output <filename> --kv-version 1|2 [--format env|yaml] [--append] ...keys" >&2
}

while [ $# -gt 0 ]; do
    case "$1" in
        "-a" | "--append")
            append="-a"
            ;;
        "-f" | "--format")
            format="$2"
            shift
            ;;
        "-h" | "--help")
            help=1
            ;;
        "--kv-version")
            kv_version="$2"
            shift
            ;;
        "-o" | "--output")
            output="$2"
            shift
            ;;
        *)
            keys+=("$1")
            ;;
    esac
    shift
done

set -e

if [ $help -eq 1 ]; then
    usage
    exit 0
fi

if [ "$output" = "" ]; then
    echo "--output <filename> is required" >&2
    usage
    exit 1
fi

if [ "$kv_version" = "1" ]; then
    data_path=".data"
elif [ "$kv_version" = "2" ]; then
    data_path=".data.data"
else
    echo "--kv-version is required and must be 1 or 2" >&2
    usage
    exit 1
fi

if [ "$format" = "yml" ]; then
    format="yaml"
fi
if [ "$format" != "env" -a "$format" != "yaml" ]; then
    echo "--format must be 'env' or 'yaml'" >&2
    usage
    exit 1
fi
if [ "$format" = "yaml" ]; then
    sep=":"
fi

for key in "${keys[@]}"; do
    echo "vault : kv=$key"
    vault kv get --format json "$key" \
        | jq -r '.data | to_entries[] | .key + "'$sep'\"" + ( .value | gsub("\""; "\\\"") ) + "\""' \
        | tee $append "$output" > /dev/null
    [ ${PIPESTATUS[0]} -eq 0 -a ${PIPESTATUS[1]} -eq 0 -a ${PIPESTATUS[2]} -eq 0 ]
    append="-a"
done

echo "file written to '$output'"
