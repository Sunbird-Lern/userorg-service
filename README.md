# sunbird-lms-service

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b963e5ed122f47b5a27b19a87d9fa6de)](https://app.codacy.com/app/sunbird-bot/sunbird-lms-service?utm_source=github.com&utm_medium=referral&utm_content=project-sunbird/sunbird-lms-service&utm_campaign=Badge_Grade_Settings)

This is the repository for Sunbird learning management system (lms) micro-service. It provides the APIs for lms
functionality of Sunbird.

The code in this repository is licensed under MIT License unless otherwise noted. Please see
the [LICENSE](https://github.com/project-sunbird/sunbird-lms-service/blob/master/LICENSE) file for details.

## User org local setup

This readme file describes how to install and start User&Org Service and set up the default organisation & user creation
in local machine.

### System Requirements:

### Prerequisites:

* Java 11

### Prepare folders for database data and logs

```shell
mkdir -p ~/sunbird-dbs/cassandra ~/sunbird-dbs/es 
export sunbird_dbs_path=~/sunbird-dbs
```

### cassandra database setup in docker:

1. we need to get the cassandra image and can be done using the below command.

```shell
docker pull cassandra:3.11.6 
```

For network, we can use the existing network or create a new network using the following command and use it.

```shell
docker network create sunbird_db_network
```

2. We need to create the cassandra instance, By using the below command we can create the same and run in a container.

```shell
docker run -p 9042:9042 --name sunbird_cassandra -v $sunbird_dbs_path/cassandra/data:/var/lib/cassandra -v $sunbird_dbs_path/cassandra/logs:/opt/cassandra/logs -v $sunbird_dbs_path/cassandra/backups:/mnt/backups --network sunbird_db_network -d cassandra:3.11.6 
```

3. We can verify the setup by running the below command, which will show the status of cassandra as up and running

```shell
docker ps -a | grep cassandra
```

## To create/load data to Cassandra

```shell
https://github.com/Sunbird-Lern/sunbird-utils/tree/release-5.3.0#readme
```
### The system environment listed below is required for cassandra connectivity with user org service.

#### System Env variables for cassandra

```shell
sunbird_cassandra_host=localhost
sunbird_cassandra_password=<your_cassandra_password>
sunbird_cassandra_port=<your_cassandra_port>
sunbird_cassandra_username=<your_cassandra_username>
```

### elastic search setup in docker:

1. we need to get the elastic search image and can be done using the below command.

```shell
docker pull elasticsearch:6.8.11
```

2. We need to create the elastic search instance, By using the below command we can create the same and run in a
   container.

```shell
docker run -p 9200:9200 --name sunbird_es -v $sunbird_dbs_path/es/data:/usr/share/elasticsearch/data -v $sunbird_dbs_path/es/logs://usr/share/elasticsearch/logs -v $sunbird_dbs_path/es/backups:/opt/elasticsearch/backup -e "discovery.type=single-node" --network sunbird_db_network -d docker.elastic.co/elasticsearch/elasticsearch:6.8.11
```

--name - Name your container (avoids generic id)

-p - Specify container ports to expose

Using the -p option with ports 7474 and 7687 allows us to expose and listen for traffic on both the HTTP and Bolt ports. Having the HTTP port means we can connect to our database with Neo4j Browser, and the Bolt port means efficient and type-safe communication requests between other layers and the database.

-d - This detaches the container to run in the background, meaning we can access the container separately and see into all of its processes.

-v - The next several lines start with the -v option. These lines define volumes we want to bind in our local directory structure so we can access certain files locally.

3. We can verify the setup by running the below command, which will show the status of elastic search as up and running

```shell
docker ps -a | grep es
```

4. This step is required only if you use ubuntu system. Make sure you create necessary permissions for the folder by
   executing the below command,

```shell
chmod -R 777 sunbird-dbs/es
```

### elastic search Indices and mappings setup

1. clone the latest branch of sunbird-utils (Clone again only If it is not cloned previously for cassandra setup) using
   below command,

```shell
git clone https://github.com/Sunbird-Lern/sunbird-utils/<latest-branch>
```

2. then navigate to,
   <project_base_path>/sunbird-utils/sunbird-es-utils/src/main/resources folder, for getting the index and mappings.
   We have to use postman to create index and mappings.
   Create index and mappings mainly for user, userfeed, usernotes, org, and location JSON files.

#### PUT {{es_host}}/<indices_name> Body : <respective_index_json_content>

For example,

```shell
curl --location --request PUT '{{es_host}}/<indices_name>/_mapping/_doc'
--header 'Content-Type: application/json'
--data '<respective_index_json_content>'
```

replace <respective_index_name> with user,userfeed,usernotes,org and location one by one along with '<
respective_index_json_content> in the body.

#### PUT {{es_host}}/<indices_name>/_mapping/_doc Body : <respective_mapping_json_content>

For example,

```shell
curl --location --request PUT '{{es_host}}/<indices_name>/_mapping/_doc'
--header 'Content-Type: application/json'
--data '<respective_mapping_json_content>'
```

replace <respective_index_name> with user,userfeed,usernotes,org and location one by one along with '<
respective_mapping_json_content> in the body.

### The system environment listed below is required for elastic search connectivity with user org service.

#### System Env variables for elastic search

```shell
sunbird_es_host=localhost
sunbird_es_port=<your es port>
sunbird_es_cluster=<your cluster name>
user_index_alias=<index name of user>
org_index_alias=<index name of org>
```

## User Org Service Setup

1. Clone the latest branch of the user-org service using the below command,

```shell
git clone https://github.com/<YOUR_FORK>/sunbird-lms-service.git
```

2. Go to the path: <project-base-path>/sunbird-lms-service and run the below maven command to build the application.

```shell
mvn clean install -DskipTests
```

Please ensure the build is success before firing the below command, if the build is not success then the project might
not be imported properly and there is some configuration issues, fix the same and rebuild until it is successful.

3. Go to the path: <project-base-path>/sunbird-lms-service/controller and run the below maven command to run the netty
   server.

```shell
mvn play2:run
```

4.Using the below command we can verify whether the databases(cassandra,elastic search) connection is established or
not. If all connections are good, health is shown as 'true' otherwise it will be 'false'.

```shell
curl --location --request GET 'http://localhost:9000/health’
```

## Pre-required Configuration to Make User/Org service Completely working:

### Organisation Creation:

1. Creating a new custodian/root organisation is mandatory. so please ensure you get a 200 OK response after creation.
   Please make sure you have disabled the required flag to skip the channel validation in externalresource.properties
   file as below,
   channel_registration_disabled=true

```shell
curl --location --request POST '{{host}}/v1/org/create' \
--header 'Content-Type: application/json' \
--data-raw '<org_create_request_payload'
```

{{host}} has to be replaced with respective local host url and <org_create_request_payload> has to be replaced with the
latest payload of ORG_CREATE_API

### Organisation Type Configuration:

```shell
curl --location '{{host}}/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data '{
"request": {
"id": "orgTypeConfig",
"field": "orgTypeConfig",
"value": "{\"fields\":[{\"name\":\"School\",\"value\":2,\"description\":\"School\",\"displayName\":\"School\",\"flagNameList\":[\"isSchool\"]},{\"name\":\"Board\",\"value\":5,\"description\":\"Board\",\"displayName\":\"Board\",\"flagNameList\":[\"isBoard\"]}]}"
}
}'
```

### Org Profile Configuration:

```shell
curl --location --request POST 'localhost:9000/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data-raw '{
"request": {
"id": "orgProfileConfig",
"field": "orgProfileConfig",
"value": "{\"csv\":{\"supportedColumns\":{\"SCHOOL NAME\":\"orgName\",\"BLOCK CODE\":\"locationCode\",\"STATUS\":\"status\",\"SCHOOL ID\":\"organisationId\",\"EXTERNAL ID\":\"externalId\",\"DESCRIPTION\":\"description\",\"ORGANISATION cTYPE\":\"organisationType\"}, \"outputColumns\": {\"organisationId\":\"SCHOOL ID\",\"orgName\":\"SCHOOL NAME\",\"locationCode\":\"BLOCK CODE\",\"locationName\":\"BLOCK NAME\",\"externalId\":\"EXTERNAL ID\",\"organisationType\":\"ORGANISATION TYPE\"}, \"outputColumnsOrder\":[\"organisationId\",\"orgName\",\"locationCode\", \"locationName\",\"externalId\",\"organisationType\"],\"mandatoryColumns\":[\"orgName\",\"locationCode\",\"status\",\"organisationType\"]}}"
}
}'
```

### Custodian channel configuration:

```shell
curl --location --request POST '{{host}}/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data-raw '{
"request": {
"id": "custodianOrgChannel",
"field": "custodianOrgChannel",
"value": "Channel"
}
}'
```

### Custodian org id configuration:

```shell
curl --location --request POST 'localhost:9000/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data-raw '{
"request": {
"id": "custodianOrgId",
"field": "custodianOrgId",
"value": "0137038836873134080"
}
}'
```

### Terms and Conditions configuration:

#### Basic TNC:

```shell
curl --location '{{host}}/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data '{
"request": {
"id": "tncConfig",
"field": "tncConfig",
"value": "{\"latestVersion\":\"v12\",\"v12\":{\"url\":\"https://obj.stage.sunbirded.org/termsandcondtions/terms-and-conditions-v12.html\"}}"
}
}'
```

#### Org Admin TNC:

```shell
curl --location '{{host}}/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data '{
"request": {
"id": "orgAdminTnc",
"field": "orgAdminTnc",
"value": "{\"latestVersion\":\"3.5.0\",\"3.5.0\":{\"url\":\"https://sunbirdstagingpublic.blob.core.windows.net/termsandcondtions/terms-and-conditions-v9.html#administratorGuidelines\"}}"
}
}'
```

#### Groups TNC:

```shell
curl --location '{{host}}/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data '{
"request": {
"id": "groupsTnc",
"field": "groupsTnc",
"value": "{\"latestVersion\":\"3.5.0\",\"3.5.0\":{\"url\":\"https://sunbirdstagingpublic.blob.core.windows.net/termsandcondtions/terms-and-conditions-v9.html#groupGuidelines\"}}"
}
}'
```

#### Report Viewer TNC:

```shell
curl --location '{{host}}/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data '{
"request": {
"id": "reportViewerTnc",
"field": "reportViewerTnc",
"value": "{\"latestVersion\":\"4.0.0\",\"4.0.0\":{\"url\":\"https://sunbirdstagingpublic.blob.core.windows.net/termsandcondtions/terms-and-conditions-v9.html#administratorGuidelines\"}}"
}
}'
```

### User Profile Configuration

```shell
curl --location --request POST 'localhost:9000/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data-raw '{
"request": {
"id": "userProfileConfig",
"field": "userProfileConfig",
"value": "{\"fields\":[\"firstName\",\"lastName\",\"profileSummary\",\"avatar\",\"countryCode\",\"dob\",\"email\",\"gender\",\"grade\",\"language\",\"location\",\"phone\",\"subject\",\"userName\",\"webPages\",\"jobProfile\",\"address\",\"education\",\"skills\",\"badgeAssertions\"],\"publicFields\":[\"firstName\",\"lastName\",\"profileSummary\",\"userName\"],\"privateFields\":[\"email\",\"phone\"],\"csv\":{\"supportedColumns\":{\"NAME\":\"firstName\",\"MOBILE PHONE\":\"phone\",\"EMAIL\":\"email\",\"SCHOOL ID\":\"orgId\",\"USER_TYPE\":\"userType\",\"ROLES\":\"roles\",\"USER ID\":\"userId\",\"SCHOOL EXTERNAL ID\":\"orgExternalId\"},\"outputColumns\":{\"userId\":\"USER ID\",\"firstName\":\"NAME\",\"phone\":\"MOBILE PHONE\",\"email\":\"EMAIL\",\"orgId\":\"SCHOOL ID\",\"orgName\":\"SCHOOL NAME\",\"userType\":\"USER_TYPE\",\"orgExternalId\":\"SCHOOL EXTERNAL ID\"},\"outputColumnsOrder\":[\"userId\",\"firstName\",\"phone\",\"email\",\"organisationId\",\"orgName\",\"userType\",\"orgExternalId\"],\"mandatoryColumns\":[\"firstName\",\"userType\",\"roles\"]},\"read\":{\"excludedFields\":[\"avatar\",\"jobProfile\",\"address\",\"education\",\"webPages\",\"skills\"]},\"framework\":{\"fields\":[\"board\",\"gradeLevel\",\"medium\",\"subject\",\"id\"],\"mandatoryFields\":[\"id\"]}}"
}
}'
```

### Once the setup done, create user using below APIs

```shell
curl --location '{{host}}/v1/user/create' \
--header 'Content-Type: application/json' \
--data-raw '<user_create_data>'
```

{{host}} has to be replaced with respective local host url and <user_create_data> has to be replaced with the latest
payload of USER_CREATE_API

If you are able to create an user successfully then the local setup is working normal.