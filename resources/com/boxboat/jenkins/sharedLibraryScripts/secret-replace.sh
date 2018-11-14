#!/bin/bash

openRe="\{\{\s*"
closeRe="\s*\}\}"
keyRe="\s*\(\s*[\'\\\"]([^\'\\\"]+)[\'\\\"]\s*,\s*[\'\\\"]([^\'\\\"]+)[\'\\\"]\s*\)\s*"

usage () {
    echo "Usage: ./secret-replace.sh [--env key=value...] ...globs" >&2
}

get_match() {
    printf "$1" | sed -rn "s/.*${keyRe}.*/\\"${2}"/p"
}

interpolate () {
    matches=$(grep -oE "${openRe}vault${keyRe}${closeRe}" "$1" | sort | uniq)
    if [ ! -z "$matches" ]; then
        echo "file  : $1"
        IFS=$'\n'
        for match in $matches; do
            matchSubstituted=$(printf "$match" | envsubst)
            kv=$(get_match "$matchSubstituted" "1")
            key=$(get_match "$matchSubstituted" "2")
            echo "vault : kv=$kv key=$key"
            value=$(vault kv get -field "$key" "$kv")
            sed -i "s|${match//$/\\$}|${value//|/\\|}|g" "$1"
        done
    fi
}

help=0
globs=()
envs=()

while [ $# -gt 0 ]; do
    case "$1" in
        "-h" | "--help")
            help=1
            ;;
        "-e" | "--env")
            envs+=("$2")
            shift
            ;;
        *)
            globs+=("$1")
            ;;
    esac
    shift
done

set -e
shopt -s globstar

if [ $help -eq 1 ]; then
    usage
    exit 0
fi

for env in "${envs[@]}"; do
    export "$env"
done

for glob in "${globs[@]}"; do
    for path in $glob; do
        if [ -f "$path" ]; then
            interpolate "$path"
        fi
    done
done
