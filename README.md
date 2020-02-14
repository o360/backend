# Open360 Backend

[![Build Status](https://travis-ci.org/o360/backend.svg?branch=master)](https://travis-ci.org/o360/backend)
[![License](http://img.shields.io/:license-Apache%202-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Coverage Status](https://coveralls.io/repos/github/o360/backend/badge.svg?branch=master)](https://coveralls.io/github/o360/backend?branch=master)

* [Dependencies](#dependencies)
* [Deployment script](#deployment-script)
* [Manual build and run](#manual-build-and-run)
* [Migrations](#migrations)
* [Configuration](#configuration)

## Dependencies
Build:
- JDK 8
- SBT 1.3+
- Docker

Services:
- PostgreSQL
- SMTP server
- Google Drive API
- Google Sheet API

## Deployment script
Script runs through the following stages:
* Test
* Coverage report
* Docker image building
* Docker image running (also applies migrations)

To run the script follow the next steps:
1. Set up an environment:

    1.1. Install and configure PostgreSQL database
        
    1.2. Set up [environment variables](#environment-variables)
    
    1.3. Create configuration files in the current directory:
    * *drive_service_key.json* - credentials for Google Drive and Google Sheets API
    ([how to get it](#setting-up-google-account));
    
        path inside container: `/opt/docker/conf/drive_service_key.json`
    * *user_approved.html* - email template for approved user email
    ([creating email template](#creating-email-template));
    
        path inside container: `/opt/docker/templates/user_approved.html`
    * *user_invited.html* - email template for user invite email
    ([creating email template](#creating-email-template));
    
        path inside container: `/opt/docker/templates/user_invited.html`

2. [Set up Google account](#setting-up-google-account)
3. Execute `./run-in-docker.sh`

## Manual build and run
Instead of running by script you may want to execute the steps manually

### Run tests

Execute `sbt clean test` or `sbt clean coverage test && sbt coverageReport` for additional coverage report generation.

Coverage report can be seen at `target/scala-2.13/scoverage-report/index.html`

### Run application
To run the application locally:
  1. Set up database and apply [migrations](#migrations)
  2. Set up configuration:
     * either by changing `conf/application.conf` file
     * or by specifying the [environment variables](#environment-variables)
  3. Create `drive_service_key.json` file as described in [Setting up Google account](#setting-up-google-account)
  4. Create `user_invited.html` and `user_approved.html` email templates.
  5. Execute `sbt clean run`

### Build Docker image
Execute `sbt docker:publishLocal`

### Run Docker container

> Migrations will be applied automatically at application startup
>
> It is possible to directly replace application.conf file with preconfigured one instead of using 
environment variables. *application.conf* path inside container - `/opt/docker/conf/application.conf`

```shell
docker run -d --name open360-api --restart=always -p 9000:9000 \
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
    -v $(pwd)/user_invited.html:/opt/docker/templates/user_invited.html \
    -v ${USER_FILES_PATH}:/opt/docker/uploads
    open360/api:latest
```
## Migrations
Database migrations are applied automatically on startup when running in docker.

If you need to manually apply migrations:
1. Get your PostgreSQL instance up and running
2. Set up ***DATABASE_USER, DATABASE_PASSWORD, DATABASE_URL*** environment variables (URL format 
`jdbc:postgresql://<host>:<port>/<db_name>`)
3. Execute `sbt flywayMigrate`
  
## Configuration

### Environment variables

 * *DATABASE_USER* - database user name
 * *DATABASE_PASSWORD* - database user password
 * *DATABASE_URL* - database url in "jdbc:postgresql://<host>:<port>/<db_name>" format
 * *APPLICATION_SECRET* - application secret used for signing JWT tokens
 * *GOOGLE_REDIRECT_URL* - allowed OAuth redirect URL, e.g. "http://localhost/login/google"
 * *GOOGLE_CLIENT_ID* - Google OAuth client ID([how to set up Google app](#dependencies))
 * *GOOGLE_CLIENT_SECRET* - Google OAuth client secret
 * *MAIL_HOST* - SMTP server address. Default is localhost
 * *MAIL_PORT* - SMTP server port. Default is 25
 * *MAIL_USER* - SMTP server username
 * *MAIL_PASSWORD* - SMTP server password
 * *MAIL_SEND_FROM* - Sender email address
 * *EXPORT_SECRET* - Secret key used for JSON export
 * *SCHEDULER_ENABLED* - (true, false) is background tasks execution enabled
 * *USER_FILES_PATH* - user files path
 
### Setting up Google account

1. Go to https://console.developers.google.com/ and create a project.
2. Getting *drive_service_key.json*: 
    * Go to credentials section
    * Press "CREATE CREDENTIALS" button and choose "Service account"
    * Create credentials without role
    * Press "Create key" and select JSON format
    * JSON file will be downloaded automatically
3. Getting Google client ID and Google client secret
    * Go to OAuth consent screen and set up an application
    * Go to credentials section
    * Press "CREATE CREDENTIALS" button and choose "OAuth client ID"
    * Choose "Web application", enter client ID name and valid redirect uris
    * Client ID and client secret will be displayed automatically after saving
    
4. Enable following APIs for project via "Library":
    * Google Drive API
    * Google Sheets API
  
### Creating email template
Application requires 2 templates on startup: `user_invited.html` and `user_approved.html`.

Each of them represents body of the corresponding message types and may contain variables:
* `user_invited.html`
    * `{{code}}` variable, representing an invite code
* `user_approved.html`
    * `{{user_name}}` variable, containing approved user name

