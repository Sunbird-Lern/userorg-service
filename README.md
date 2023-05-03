# sunbird-lms-service

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b963e5ed122f47b5a27b19a87d9fa6de)](https://app.codacy.com/app/sunbird-bot/sunbird-lms-service?utm_source=github.com&utm_medium=referral&utm_content=project-sunbird/sunbird-lms-service&utm_campaign=Badge_Grade_Settings)

This is the repository for User and Org micro-service. It provides the APIs for User and Org
functionality of Sunbird.

The code in this repository is licensed under MIT License unless otherwise noted. Please see
the [LICENSE](https://github.com/project-sunbird/sunbird-lms-service/blob/master/LICENSE) file for details.

## User org development environment setup:

This readme file describes how to install and start User&Org Service and set up the default organisation & user creation
in local machine.

### System Requirements:

### Prerequisites:

* Java 11
* Docker - Latest
* Maven - Latest

### Prepare folders for database data and logs
#### Command
```shell
mkdir -p ~/sunbird-dbs/cassandra ~/sunbird-dbs/es 
export sunbird_dbs_path=~/sunbird-dbs
```
#### Verification
```shell
echo $sunbird_dbs_path
```
### cassandra database setup in docker:

1. we need to get the cassandra image and can be done using the below command.
#### Command to pull cassandra docker image
```shell
docker pull cassandra:3.11.6 
```
For network, we can use the existing network or create a new network using the following command and use it.
#### Command to create a docker network
```shell
docker network create sunbird_db_network
```

2. We need to create the cassandra instance, By using the below command we can create the same and run in a container.
#### Command to start Cassandra docker container
```shell
docker run -p 9042:9042 --name sunbird_cassandra \
 -v $sunbird_dbs_path/cassandra/data:/var/lib/cassandra \
 -v $sunbird_dbs_path/cassandra/logs:/opt/cassandra/logs \
 -v $sunbird_dbs_path/cassandra/backups:/mnt/backups \
 --network sunbird_db_network -d cassandra:3.11.6 
```

3. We can verify the setup by running the below command, which will show the status of cassandra as up and running
#### Command to validate
```shell
docker ps -a | grep cassandra
```

## To create/load keyspaces and tables to Cassandra
<p style="color:#ff0000;">Don't forget to click the link and follow the below setup as it is mandatory</p>

#### click the link [sunbird-utils-cassandra-setup](https://github.com/Sunbird-Lern/sunbird-utils/tree/release-5.3.0#readme) and follow the steps for creating/loading the cassandra keyspaces and tables to your developement environment

4.We can verify the creation of keyspaces and tables by connecting to the cassandra docker container using ssh
#### Command to verify the creation of keyspaces and tables
```shell
docker exec -it sunbird_cassandra /bin/bash
```

### elastic search setup in docker:

1. We need to obtain the Elasticsearch image, which can be achieved using the command below.
#### Command to pull elasticsearch docker image
```shell
docker pull elasticsearch:6.8.11
```

2. We need to create an ElasticSearch instance, and we can do so by running the following command to create and run 
it in a container.
#### Command to start elasticsearch docker container
```shell
docker run -p 9200:9200 --name sunbird_es \
 -v $sunbird_dbs_path/es/data:/usr/share/elasticsearch/data \
 -v $sunbird_dbs_path/es/logs://usr/share/elasticsearch/logs \
 -v $sunbird_dbs_path/es/backups:/opt/elasticsearch/backup \
 -e "discovery.type=single-node" --network sunbird_db_network \
 -d docker.elastic.co/elasticsearch/elasticsearch:6.8.11
```

>"-p 9200:9200" maps the host's port 9200 to the container's port 9200, allowing access to the Elasticsearch API.
>
>"--name sunbird_es" assigns the name "sunbird_es" to the container, which can be used to reference it in other Docker commands.
>
>"-v $sunbird_dbs_path/es/data:/usr/share/elasticsearch/data" mounts the host's directory "$sunbird_dbs_path/es/data" as the Elasticsearch data directory inside the container.
>
>"-v $sunbird_dbs_path/es/logs://usr/share/elasticsearch/logs" mounts the host's directory "$sunbird_dbs_path/es/logs" as the Elasticsearch logs directory inside the container.
>
>"-v $sunbird_dbs_path/es/backups:/opt/elasticsearch/backup" mounts the host's directory "$sunbird_dbs_path/es/backups" as the Elasticsearch backups directory inside the container.
>
>"-e "discovery.type=single-node"" sets an environment variable "discovery.type" with the value "single-node", which tells Elasticsearch to start as a single-node cluster.
>
>"--network sunbird_db_network" assigns the container to the Docker network "sunbird_db_network", which is used to connect the container to other containers in the same network.
>
>"-d" runs the container in detached mode, which allows it to run in the background.

3. To verify the setup, run the following command, which will display the elastic search status as up and running.
```shell
docker ps -a | grep es
```

4. This step is required only if you use ubuntu system. Make sure you create necessary permissions for the folder by
   executing the below command,
#### Command to validate
```shell
chmod -R 777 sunbird-dbs/es
```

### elastic search Indices and mappings setup

Create indices for,
1. [user](https://github.com/project-sunbird/sunbird-devops/blob/release-5.3.0-lern/ansible/roles/es-mapping/files/indices/userv3.json)
2. [userfeed](https://github.com/project-sunbird/sunbird-devops/blob/release-5.3.0-lern/ansible/roles/es-mapping/files/indices/userfeed.json)
3. [usernotes](https://github.com/project-sunbird/sunbird-devops/blob/release-5.3.0-lern/ansible/roles/es-mapping/files/indices/usernotes.json)
4. [org](https://github.com/project-sunbird/sunbird-devops/blob/release-5.3.0-lern/ansible/roles/es-mapping/files/indices/orgv3.json)
5. [location](https://github.com/project-sunbird/sunbird-devops/blob/release-5.3.0-lern/ansible/roles/es-mapping/files/indices/location.json)

#### PUT {{es_host}}/<indices_name> Body : <respective_index_json_content> 
#### replace <indices_name> with
##### user,userfeed,usernotes,org and location
one by one along with copying
<respective_index_json_content> with the link provided in previous step in the body.

For example,
```shell
curl --location --globoff --request PUT 'localhost:9200/location' \
--header 'Content-Type: application/json' \
--data '<respective_index_json_content>'
```

Create mappings for,
1. [user](https://github.com/project-sunbird/sunbird-devops/blob/release-5.3.0-lern/ansible/roles/es-mapping/files/mappings/userv3-mapping.json)
2. [userfeed](https://github.com/project-sunbird/sunbird-devops/blob/release-5.3.0-lern/ansible/roles/es-mapping/files/mappings/userfeed-mapping.json)
3. [usernotes](https://github.com/project-sunbird/sunbird-devops/blob/release-5.3.0-lern/ansible/roles/es-mapping/files/mappings/usernotes-mapping.json)
4. [org](https://github.com/project-sunbird/sunbird-devops/blob/release-5.3.0-lern/ansible/roles/es-mapping/files/mappings/orgv3-mapping.json)
5. [location](https://github.com/project-sunbird/sunbird-devops/blob/release-5.3.0-lern/ansible/roles/es-mapping/files/mappings/location-mapping.json)


#### PUT {{es_host}}/<indices_name>/_mapping/_doc Body : <respective_mapping_json_content>

For example,

```shell
curl --location --request PUT 'localhost:9200/location/_mapping/_doc' \
--header 'Content-Type: application/json' \
--data '<respective_mapping_json_content>'
```

#### replace <respective_index_name> with
##### user,userfeed,usernotes,org and location
one by one along with copying
<respective_mapping_json_content>with the link provided in previous step in the body.

### The system environment listed below is required for elastic search connectivity with user org service.

## User Org Service Repository Setup

1. Clone the latest branch of the user-org service using the below command,
#### command to clone the repository
```shell
git clone https://github.com/<YOUR_FORK>/sunbird-lms-service.git
```
2.To set up the necessary environment variables,
Go to the path: <project-base-path>/sunbird-lms-service and please run the following script.
#### command to export the configuration
```shell
./scripts/userorg-config.sh
```
3. Go to the path: <project-base-path>/sunbird-lms-service and run the below maven command to build the application.
#### Command to build the code base
```shell
mvn clean install -DskipTests
```
Please ensure the build is success before firing the below command, if the build is not success then the project might
not be imported properly and there is some configuration issues, fix the same and rebuild until it is successful.
4. Go to the path: <project-base-path>/sunbird-lms-service/controller and run the below maven command to run the netty
   server.
#### Command to run the service
```shell
mvn play2:run
```
5.Using the below command we can verify whether the databases(cassandra,elastic search) connection is established or
not. If all connections are good, health is shown as 'true' otherwise it will be 'false'.
#### Command to check the setup using health API
```shell
curl --location --request GET 'http://localhost:9000/health’
```

## Pre-required Configuration to Make User/Org service Completely working:
<p style="color:#ff0000;">Don't forget to click the link and follow the below setup as it is mandatory</p>

#### click the link [pre-required configuration setup](https://github.com/Sunbird-Lern/sunbird-lms-service/blob/release-5.3.0/lernsetup.md)
and follow the steps for setting up pre required configuration to Make User/Or.g service Completely working