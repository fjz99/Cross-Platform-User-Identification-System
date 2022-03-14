#!/usr/bin/env bash

cd ..

mvn package

docker-compose -f ./docker/docker-compose.yml build
docker-compose -f ./docker/docker-compose.yml up -d
