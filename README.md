# BW Staff Assessment System Backend
## Dependencies
- java
- sbt
- docker

## How to

### Run tests
Execute `sbt clean test`

### Run tests with coverage report
Execute `sbt clean coverage test && sbt coverageReport`
Coverage report will be created at `target/scala-2.11/coverage-report/cobertura.xml`

### Apply db migrations
Database migrations are applied automatically in prod mode. 
In order to manually apply migrations execute `sbt flywayMigrate`

> ***DATABASE_USER, DATABASE_PASSWORD, DATABASE_URL*** environment
> variables must have been set and database must have been created

### Build docker image
Execute `sbt docker:publishLocal`

### Run container
```shell
docker run -d --name bw-assessment-api --restart=always -p 9000:9000 \
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
    -e SCHEDULER_ENABLED=true \
    -v $(pwd)/drive_service_key.json:/opt/docker/conf/drive_service_key.json \
    -v $(pwd)/user_approved.html:/opt/docker/templates/user_approved.html \
    bw-assessment/api:latest
```
> It is possible to directly replace application.conf file with preconfigured one instead of using 
environment variables. *application.conf* path inside container - `/opt/docker/conf/application.conf`

> Migrations will be applied automatically at application startup

### Test + coverage + migrations + image building + image running
Setup environment and execute `./run-in-docker.sh`
See *Set up environment* for details

### Setup environment
1. Install and configure postgres database. Sample init script:
	```
	CREATE DATABASE <dbname>;
	CREATE USER <username> WITH password '<password>';
    ```
2. Set environment variables
    * *DATABASE_USER* - database user name
    * *DATABASE_PASSWORD* - database user password
    * *DATABASE_URL* - database url in "jdbc:postgresql://host:port/db_name" format
    * *APPLICATION_SECRET* - application secret used for signing JWT tokens
    * *GOOGLE_REDIRECT_URL* - allowed OAuth redirect URL, e.g. "http://localhost/login/google"
    * *GOOGLE_CLIENT_ID* - google OAuth client ID
    * *GOOGLE_CLIENT_SECRET* - google OAuth client secret
    * *MAIL_HOST* - SMTP server address. Default is localhost
    * *MAIL_PORT* - SMTP server port. Default is 25
    * *MAIL_USER* - SMTP server username
    * *MAIL_PASSWORD* - SMTP server password
    * *MAIL_SEND_FROM* - Sender email address
    * *EXPORT_SECRET* - Secret key used for JSON export
    * *SCHEDULER_ENABLED* - (true, false) is background tasks execution enabled
3. Create configuration files
    * *drive_service_key.json* - credentials for Google Drive and Google Sheets API; 
    path inside container: `/opt/docker/conf/drive_service_key.json;`
    * *user_approved.html* - email template for approved user email;
    path inside container: `/opt/docker/templates/user_approved.html`

### Setup Google account
1. Go to https://console.developers.google.com/ and create a project.
2. Getting *drive_service_key.json*: 
    * Go to credentials section
    * Press "create credentials" button and choose "service account"
    * Select JSON format
    * Create credentials without role
    * JSON file will be downloaded automatically
3. Getting google client ID and google client secret
    * Go to credentials section
    * Press "create credentials" button and choose "OAuth client ID"
    * Choose web application, enter client ID name and valid redirect uris
    * Client ID and client secret will be displayed automatically after saving
4. Enable following APIs for project
    * Google+ API
    * Google Drive API
    * Google Sheets API
