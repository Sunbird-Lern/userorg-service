#!/bin/sh
# Build script
# set -o errexit

commit_hash=$1
name=player
version=$2
node=$3
org=$4

docker build -f ./Dockerfile --label commitHash=$(git rev-parse --short HEAD) -t ${org}/${name}:${version}_${commit_hash} .
echo {\"image_name\" : \"${name}\", \"image_tag\" : \"${version}_${commit_hash}\", \"node_name\" : \"$node\"} > metadata.json
