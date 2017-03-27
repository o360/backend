#!/usr/bin/env bash

set -xe

if [[ \
-z $DATABASE_USER || \
-z $DATABASE_PASSWORD || \
-z $DATABASE_URL || \
-z $APPLICATION_SECRET \
 ]];
then
echo "ENVIRONMENT VARIABLES ARE UNSET"
exit 1
fi

sbt clean coverage test
sbt coverageReport
sbt flywayMigrate
sbt docker:publishLocal

docker rm -f private-bw-assessment-api || true

docker run -d --name private-bw-assessment-api --restart=always -p 9000:9000 \
	-e DATABASE_USER=${DATABASE_USER} \
    -e DATABASE_PASSWORD=${DATABASE_PASSWORD} \
    -e DATABASE_URL=${DATABASE_URL} \
    -e APPLICATION_SECRET=${APPLICATION_SECRET} \
    bw-assessment/api:latest
