## Pre-requisites
1. Akka middleware actors should be up and running. Follow these [instructions](https://github.com/project-sunbird/sunbird-lms-mw/blob/master/setup.md) to run the akka actors.

## Configuration
1. Environment Variabls
    1. sunbird_learnerstate_actor_host: host of the actor service, e.g.: actor-service
    2. sunbird_learnerstate_actor_port: port on which the remote actors are running
    3. sunbird_sso_publickey : sso public key
    4. sunbird_sso_url: url for keycloak server
    5. sunbird_sso_realm: keycloak realm name
    6. sunbird_sso_username: keycloak user name
    7. sunbird_sso_password: keycloak password
    8. sunbird_sso_client_id: key cloak client id
    9. sunbird_sso_client_secret : keycloak client secret (not mandatory)

## Build
1. Run "git submodule foreach git pull origin master" to pull the latest sunbird-common submodule.
2. Run "mvn clean install" to build the services.
2. Go to "service" and run the command "mvn play2:dist" to generate the dist file for services.
3. The build file "learning-service-1.0-SNAPSHOT-dist.zip" is generated in "sunbird-lms-service/service/target" folder.

## Run
1. Unzip the dist file "learning-service-1.0-SNAPSHOT-dist.zip".
2. Run the command "java -cp 'learning-service-1.0-SNAPSHOT/lib/*' play.core.server.ProdServerStart learning-service-1.0-SNAPSHOT" to start the service.
