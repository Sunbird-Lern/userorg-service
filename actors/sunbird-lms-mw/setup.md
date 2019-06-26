## Pre-requisites
1. [Setup Cassandra](#setup-cassandra)
2. [Setup Elasticsearch](#setup-elasticsearch)
3. [Set Configuration](#set-configurations)

### Setup Cassandra
1. Install Cassandra database and start the server
2. Run [cassandra.cql](https://github.com/project-sunbird/sunbird-lms-mw/blob/master/actors/src/main/resources/cassandra.cql) file to create the required keyspaces, tables and indices
3. Copy pageMgmt.csv and pageSection.csv to a temp folder on cassandra machine. 
    - e.g.: /tmp/cql/pageMgmt.csv and /tmp/cql/pageSection.csv.
4. Execute the below commands. 
```cql
cqlsh -e "COPY sunbird.page_management(id, appmap,createdby ,createddate ,name ,organisationid ,portalmap ,updatedby ,updateddate ) FROM '/tmp/cql/pageMgmt.csv'"
cqlsh -e "COPY sunbird.page_section(id, alt,createdby ,createddate ,description ,display ,imgurl ,name,searchquery , sectiondatatype ,status , updatedby ,updateddate) FROM '/tmp/cql/pageSection.csv'"
```
    
### Setup Elasticsearch
1. Install ElasticSearch database and start the server


### Set Configurations

Below are the list of environment variables to setup.

| variable                              | description                                                                                                 |
|---------------------------------------|-------------------------------------------------------------------------------------------------------------|
| sunbird_cassandra_host                | host running the Cassandra server                                                                           |
| sunbird_cassandra_port                | port on which cassandra server is running                                                                   |
| sunbird_cassandra_username (optional) | username for Cassandra database, if authentication is enabled                                               |
| sunbird_cassandra_password (optional) | password for Cassandra database, if authentication is enabled                                               |
| sunbird_es_host                       | host running the Elasticsearch server                                                                       |
| sunbird_es_port                       | port on which Elasticsearch server is running                                                               |
| sunbird_es_cluster (optional)         | name of the Elasticsearch cluster                                                                           |
| sunbird_learner_actor_host            | host running for learner actor                                                                              |
| sunbird_learner_actor_port            | port on which learner actor is running                                                                      |
| sunbird_sso_url                       | url for keycloak server                                                                                     |
| sunbird_sso_realm                     | keycloak realm name                                                                                         |
| sunbird_sso_username                  | keycloak user name                                                                                          |
| sunbird_sso_password                  | keycloak password                                                                                           |
| sunbird_sso_client_id                 | key cloak client id                                                                                         |
| sunbird_sso_client_secret             | keycloak client secret (not mandatory)                                                                      |
| ekstep_content_search_base_url        | provide base url for EkStep content search                                                                  |
| ekstep_authorization                  | provide Authorization for value for content search                                                          |
| sunbird_pg_host                       | Postgres host name or ip                                                                                    |
| sunbird_pg_port                       | Postgres port number                                                                                        |
| sunbird_pg_db                         | Postgres db name                                                                                            |
| sunbird_pg_user                       | Postgres db user name                                                                                       |
| sunbird_pg_password                   | Postgress db password                                                                                       |
| sunbird_installation                  |                                                                                                             |
| ekstep_api_base_url                   |                                                                                                             |
| sunbird_mail_server_host              |                                                                                                             |
| sunbird_mail_server_port              |                                                                                                             |
| sunbird_mail_server_username          |                                                                                                             |
| sunbird_mail_server_password          |                                                                                                             |
| sunbird_mail_server_from_email        |                                                                                                             |
| sunbird_account_name                  | account name of azure blob storage                                                                          |
| sunbird_account_key                   | azure blob storage account key                                                                              |
| sunbird_quartz_mode                   | put this value {"embedded" to run quartz without any data base, "any other value" to run with postgres db } |
| sunbird_encryption_key                |                                                                                                             |
| sunbird_encryption_mode               | mode value is either local or remote                                                                        |
| sunbird_sso_publickey                 | SSO public key                                                                                              |
| sunbird_env_logo_url                  | logo url for sending email.(http://www.paramountias.com/media/images/current-affairs/diksha-portal.jpg)     |
| sunird_web_url                        | web page url                                                                                                |
| sunbird_app_url                       | Play store url to download the app                                                                          |
| sunbird_msg_91_auth                   | msg 91 auth  
| sunbird_msg_sender                    | message sender name  
| sunbird_installation_email            | email of admin per installation
| sunbird_lms_base_url                  | sunbird lms service based url
| sunbird_lms_authorization             | api gateway auth key


- Do the below env setup, if you are planing to run background actor in remote mode.

| Variable                      | Description                                       |
|-------------------------------|---------------------------------------------------|
| sunbird_background_actor_host | host running for learner background actor         |
| sunbird_background_actor_port | port on which learner background actor is running |

    
- Do the below env setup, to start a actor system on a machine.

| Variable                  | Description                |
|---------------------------|----------------------------|
| sunbird_actor_system_name | actor system name to start |

Below are the details of the actor systems.

| Actor System Type                     | Description                                      |
|---------------------------------------|--------------------------------------------------|
| RemoteMiddlewareActorSystem           | It will start normal ActorSystem on that machine |
| BackGroundRemoteMiddlewareActorSystem | It will start background actor                   |

## Build
1. Clone the repository (sunbird-lms-mw)[https://github.com/project-sunbird/sunbird-lms-mw]
1. Run `git submodule foreach git pull origin master` to pull the latest sunbird-utils submodule
2. Run "mvn clean install" to build the actors.
3. The build file is a executable jar file **learner-actor-1.0-SNAPSHOT.jar** generated in `sunbird-lms-mw/actors/target` folder

## Run
Actors can be started by running the below statement.

```shell
java -cp "learner-actor-1.0-SNAPSHOT.jar" org.sunbird.learner.Application
```
    
