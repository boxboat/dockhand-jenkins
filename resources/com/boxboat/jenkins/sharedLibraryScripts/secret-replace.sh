#!/bin/bash

set -e
shopt -s globstar

openRe="\{\{\s*"
closeRe="\s*\}\}"
keyRe="\s*\(\s*[\'\\\"]([^\'\\\"]+)[\'\\\"]\s*,\s*[\'\\\"]([^\'\\\"]+)[\'\\\"]\s*\)\s*"

get_match() {
    printf "$1" | sed -rn "s/.*${keyRe}.*/\\"${2}"/p"
}

interpolate () {
    matches=$(grep -oE "${openRe}vault${keyRe}${closeRe}" "$1" | sort | uniq)
    if [ ! -z "$matches" ]; then
        echo "file  : $1"
        for match in "$matches"; do
            kv=$(get_match "$match" "1")
            key=$(get_match "$match" "2")
            echo "vault : kv=$kv key=$key"
            value=$(vault kv get -field "$key" "$kv")
            sed -i "s|$match|${value//|/\\|}|g" "$1"
        done
    fi
}

for glob in "$@"; do
    for path in $glob; do
        if [ -f "$path" ]; then
            interpolate "$path"
        fi
    done
done
