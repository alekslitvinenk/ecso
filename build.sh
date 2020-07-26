#!/usr/bin/env bash

sbt ';clean ;assembly'

docker build -f Dockerfile -t alekslitvinenk/ecso ./target/scala-2.12
docker push alekslitvinenk/ecso