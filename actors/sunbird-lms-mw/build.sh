#!/bin/sh
# Build script
# set -o errexit
e () {
    echo $( echo ${1} | jq ".${2}" | sed 's/\"//g')
}
m=$(./metadata.sh)

org=$(e "${m}" "org")
name=$(e "${m}" "name")
version=$(e "${m}" "version")
# docker build -f "./learner-actors/actors/Dockerfile.test" -t sunbird/actor-service:0.0.1-build .
# docker run --name actor-service-0.0.1-build sunbird/actor-service:0.0.1-build
# containerid=`docker ps -q -a -f name=actor-service-0.0.1-build`
# docker cp $containerid:/opt/learner-actors/actors/learner-actor/target/learner-actor-1.0-SNAPSHOT.jar ./learner-actors/actors/learner-actor-1.0-SNAPSHOT.jar
#docker rm $containerid
docker build -f ./Dockerfile -t ${org}/${name}:${version}-bronze .
