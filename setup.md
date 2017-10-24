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
## Do the below env setup , if you are planing to run actor as local mode.
    1. sunbird_cassandra_host: host running the cassandra server
    2. sunbird_cassandra_port: port on which cassandra server is running
    3. sunbird_cassandra_username (optional): username for cassandra database, if authentication is enabled
    4. sunbird_cassandra_password (optional): password for cassandra database, if authentication is enabled
    5. sunbird_es_host: host running the elasticsearch server
    6. sunbird_es_port: port on which elasticsearch server is running
    7. sunbird_es_cluster (optional): name of the elasticsearch cluster
    8. sunbird_learner_actor_host: host running for learner actor
    9. sunbird_learner_actor_port: port on which learner actor is running.
    10. ekstep_content_search_base_url : provide base url for EkStep content search
    11. ekstep_authorization : provide authorization for value for content search
    12. sunbird_pg_host: postgres host name or ip
    13. sunbird_pg_port: postgres port number
    14. sunbird_pg_db: postgres db name
    15. sunbird_pg_user: postgres db user name
    16. sunbird_pg_password: postgress db password 
    17. sunbird_installation
    18. ekstep_api_base_url
    19. sunbird_mail_server_host
    20. sunbird_mail_server_port
    21. sunbird_mail_server_username
    22. sunbird_mail_server_password
    23. sunbird_mail_server_from_email
    24. sunbird_account_name : account name of azure blob storage.
    25. sunbird_account_key : azure blob storage account key
    26. sunbird_quartz_mode: put this value {"embedded" to run quartz without any data base, "any other value" to run with postgres db }
    27. sunbird_encryption_key
    28. sunbird_encryption_mode : mode value is either local or remote
    29. sunbird_sso_publickey : sso public key
    30. sunbird_env_logo_url : logo url for sending email.(http://www.paramountias.com/media/images/current-affairs/diksha-portal.jpg) 
    31. sunird_web_url : web page url
    32. sunbird_app_url : paly store url to downlaod the app    
    

## Build
1. Run "git submodule foreach git pull origin master" to pull the latest sunbird-common submodule.
2. Run "mvn clean install" to build the services.
2. Go to "service" and run the command "mvn play2:dist" to generate the dist file for services.
3. The build file "learning-service-1.0-SNAPSHOT-dist.zip" is generated in "sunbird-lms-service/service/target" folder.

## Run
1. Unzip the dist file "learning-service-1.0-SNAPSHOT-dist.zip".
2. Run the command "java -cp 'learning-service-1.0-SNAPSHOT/lib/*' play.core.server.ProdServerStart learning-service-1.0-SNAPSHOT" to start the service.
