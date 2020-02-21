# Open360 Backend

[![Build Status](https://travis-ci.org/o360/backend.svg?branch=master)](https://travis-ci.org/o360/backend)
[![License](http://img.shields.io/:license-Apache%202-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Coverage Status](https://coveralls.io/repos/github/o360/backend/badge.svg?branch=master)](https://coveralls.io/github/o360/backend?branch=master)

* [Dependencies](#dependencies)
* [Deployment script](#deployment-script)
* [Manual build and run](#manual-build-and-run)
* [Migrations](#migrations)
* [Configuration](#configuration)
* [Contributing](#contributing)

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
1. [Set up Google account](#setting-up-google-account)

2. Set up an environment:

    2.1. Install and configure PostgreSQL database

    2.2. Set up [auth sources](#setting-up-authentication-sources)

    2.3. Set up [environment variables](#environment-variables)

    2.4. Create configuration files in the current directory:
    * *drive_service_key.json* - credentials file for Google Drive and Google Sheets API
    that you downloaded [before](#setting-up-google-account);

        path inside container: `/opt/docker/conf/drive_service_key.json`
    * *user_approved.html* - email template for approved user email
    ([creating email template](#creating-email-template));

        path inside container: `/opt/docker/templates/user_approved.html`
    * *user_invited.html* - email template for user invite email
    ([creating email template](#creating-email-template));

        path inside container: `/opt/docker/templates/user_invited.html`

3. Execute `./run-in-docker.sh`

## Manual build and run
Instead of running by script you may want to execute the steps manually

### Run tests

Execute `sbt clean test` or `sbt clean coverage test && sbt coverageReport` for additional coverage report generation.

Coverage report can be seen at `target/scala-2.13/scoverage-report/index.html`

### Run application
To run the application locally:

1. Create `drive_service_key.json` file as described in [Setting up Google account](#setting-up-google-account)

2. Set up database and apply [migrations](#migrations)

3. Set up [auth sources](#setting-up-authentication-sources)

4. Set up configuration:
    * either by changing `conf/application.conf` file
    * or by specifying the [environment variables](#environment-variables)

5. Create `user_invited.html` and `user_approved.html` email templates

6. Execute `sbt clean run`

### Build Docker image
Execute `sbt docker:publishLocal`

### Run Docker container

> Migrations will be applied automatically at application startup
>
> It is possible to directly replace application.conf file with preconfigured one instead of using 
environment variables. *application.conf* path inside container - `/opt/docker/conf/application.conf`

```shell
docker run -d --name open360-api --restart=always -p 9000:9000 \
    -e DATABASE_USER \
    -e DATABASE_PASSWORD \
    -e DATABASE_URL \
    -e APPLICATION_SECRET \
    -e EXTERNAL_AUTH_SERVER_URL \
    -e GOOGLE_REDIRECT_URL \
    -e GOOGLE_CLIENT_ID \
    -e GOOGLE_CLIENT_SECRET \
    -e MAIL_HOST \
    -e MAIL_PORT \
    -e MAIL_USER \
    -e MAIL_PASSWORD \
    -e MAIL_SEND_FROM \
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

3. Enable following APIs for project via "Library":
    * Google Drive API
    * Google Sheets API

### Setting up authentication sources

For the application to able to authenticate its users, you also need to configure authentication sources. There are
currently two types of authentication sources supported (you may set up one or both):
1. Authentication using social providers:

    Table below contains all supported social providers as well as the links to how to set them up. You can set up any number of those. Please note that each provider also requires you to configure a set of additional environment variables, in addition to those listed in the [corresponding section](#environment-variables). 

    | Name  | How to set up |
    |---|---|
    | Google | [Link](#google) |
2. Custom authentication using external HTTP server:

    If you have your own database of users, which can be used to authenticate using login
    and password, you can configure the application to send the provided credentials to your
    HTTP server.

    Your HTTP server should implement only one endpoint that expects HTTP POST request with
    JSON body. The JSON that will be sent to you server contains the following fields:
    * *username* : *string*
    * *password* : *string*

    Your server shall use these values to authenticate the user using any method that makes
    sense in your domain. This way you can implement authentication with LDAP or simple SQL
    database with encrypted passwords, a third party storage or anything else. In the HTTP
    response your server should return *200 OK* code if credentials are correct and the user is
    authenticated with json response that contains the following fields:
    * *userId* : *string*, *required* - a unique user identifier. It can be an email,
    a database generated id, a uuid or something else that you use to identify users
    in your system.
    * *firstName* : *string*, *optional* - user's first name.
    * *lastName* : *string*, *optional* - user's last name.
    * *email* : *string*, *optional* - user's email.
    * *gender* : *string*, *optional*  *{'f', 'm', 'female', 'male', 'man', 'woman'}* - user's gender.

    It is strongly encouraged to provide all the values from this list, even optional ones, because
    some features, like email notification sending, filering and sorting by email/name/gender
    will not be supported if the corresponding fields are not provided.

    Any other HTTP response code is interpreted as an authentication failure.

    Here is how your server should be used by curl:

    ```shell
    $ curl --request POST 'https://your-server-domain:9090/auth' --header 'Content-Type: application/json' --data-raw '{"username":"johndoe@example.com", "password":"blackcat"}'
    > {"userId": "1", "firstName": "John", "lastName": "Doe", "email": "johndoe@example.com", "gender": "m"}
    ```

    Take a look at this [example python script](auth-server-example.py) to get an idea of how your
    HTTP server should behave.

    >❗Please make sure that the connection between the application and your script is *secure*, i.e.
    >they should either both be in the private network or use HTTPS, because passwords are sent *not* encrypted.

    Once you have set your HTTP server up, specify an additional environment variable *EXTERNAL_AUTH_SERVER_URL*
    to a string cotnaining full HTTP url for your authentication endpoint. For the above curl example, this
    endpoint shall be `https://your-server-domain:9090/auth`

#### Google
1. Go to https://console.developers.google.com/ and choose the project you created [before](#setting-up-google-account).
2. Go to OAuth consent screen and set up an application
3. Go to credentials section
4. Press "CREATE CREDENTIALS" button and choose "OAuth client ID"
5. Choose "Web application", enter client ID name and valid redirect uris
6. Client ID and client secret will be displayed automatically after saving

    ##### Additional environment variables:
    * *GOOGLE_REDIRECT_URL* - allowed OAuth redirect URL, e.g. "http://localhost/login/google"
    * *GOOGLE_CLIENT_ID* - Google OAuth client ID
    * *GOOGLE_CLIENT_SECRET* - Google OAuth client secret

### Creating email template
Application requires 2 templates on startup: `user_invited.html` and `user_approved.html`.

Each of them represents body of the corresponding message types and may contain variables:
* `user_invited.html`
    * `{{code}}` variable, representing an invite code
* `user_approved.html`
    * `{{user_name}}` variable, containing approved user name

## Contributing

If you are ready to contribute to this project, please get familiar with [contributing guide](/CONTRIBUTING.md).
