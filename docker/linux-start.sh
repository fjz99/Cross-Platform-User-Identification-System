#!/usr/bin/env bash

cd ..

a=$( mvn -q -N -Dexec.executable="echo"  -Dexec.args='${project.version}'  org.codehaus.mojo:exec-maven-plugin:1.3.1:exec)
b=$( mvn -q -N -Dexec.executable="echo"  -Dexec.args='${project.artifactId}'  org.codehaus.mojo:exec-maven-plugin:1.3.1:exec)
export JAR_NAME=${b}-${a}

echo JAR_NAME="$JAR_NAME"

docker-compose -f ./docker/docker-compose.yml up -d
