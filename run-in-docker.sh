#!/usr/bin/env bash

set -xe

if [[ \
-z $DATABASE_USER || \
-z $DATABASE_PASSWORD || \
-z $DATABASE_URL || \
-z $GOOGLE_REDIRECT_URL || \
-z $GOOGLE_CLIENT_ID || \
-z $GOOGLE_CLIENT_SECRET || \
-z $APPLICATION_SECRET || \
-z $MAIL_SEND_FROM || \
-z $EXPORT_SECRET || \
-z $USER_FILES_PATH \
 ]];
then
echo "ENVIRONMENT VARIABLES ARE UNSET"
exit 1
fi

if [ ! -f drive_service_key.json ]; then
echo "drive_service_key.json NOT FOUND"
exit 1
fi

if [ ! -f user_approved.html ]; then
echo "user_approved.html NOT FOUND"
fi

if [ ! -f user_invited.html ]; then
echo "user_invited.html NOT FOUND"
fi

sbt clean coverage test
sbt coverageReport
sbt flywayMigrate
sbt docker:publishLocal

docker rm -f private-open360-api || true

docker run -d --name private-open360-api --restart=always -p 9000:9000 \
	-e DATABASE_USER=${DATABASE_USER} \
    -e DATABASE_PASSWORD=${DATABASE_PASSWORD} \
    -e DATABASE_URL=${DATABASE_URL} \
    -e APPLICATION_SECRET=${APPLICATION_SECRET} \
    -e GOOGLE_REDIRECT_URL=${GOOGLE_REDIRECT_URL} \
    -e GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID} \
    -e GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET} \
    -e MAIL_HOST=${MAIL_HOST} \
    -e MAIL_PORT=${MAIL_PORT} \
    -e MAIL_USER=${MAIL_USER} \
    -e MAIL_PASSWORD=${MAIL_PASSWORD} \
    -e MAIL_SEND_FROM=${MAIL_SEND_FROM} \
    -e EXPORT_SECRET=${EXPORT_SECRET} \
    -e SCHEDULER_ENABLED=true \
    -v $(pwd)/drive_service_key.json:/opt/docker/conf/drive_service_key.json \
    -v $(pwd)/user_approved.html:/opt/docker/templates/user_approved.html \
    -v $(pwd)/user_invited.html:/opt/docker/templates/user_invited.html \
    -v ${USER_FILES_PATH}:/opt/docker/uploads \
    open360/api:latest
