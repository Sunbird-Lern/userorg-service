## Pre-requisites
1. Pekko middleware actors should be up and running. Follow these [instructions](https://github.com/project-sunbird/sunbird-lms-mw/blob/master/setup.md) to run the pekko actors.

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
   10. sunbird_valid_badge_subtypes: list of valid badge subtypes (comma separated)
   11. sunbird_valid_badge_roles: list of valid badge roles (comma separated)
   12. sunbird_learner_service_url: url for userorg service
## Do the below env setup , if you are planing to run actor as local mode.
    1. sunbird_cassandra_host: host running the cassandra server
    2. sunbird_cassandra_port: port on which cassandra server is running
    3. sunbird_cassandra_username (optional): username for cassandra database, if authentication is enabled
    4. sunbird_cassandra_password (optional): password for cassandra database, if authentication is enabled
    5. sunbird_es_host: host running the elasticsearch server
    6. sunbird_es_port: port on which elasticsearch server is running
    7. sunbird_es_cluster (optional): name of the elasticsearch cluster
    8. sunbird_learner_actor_host: host running for userorg actor
    9. sunbird_learner_actor_port: port on which userorg actor is running.
    10. ekstep_content_search_base_url : provide base url for EkStep content search
    11. ekstep_authorization : provide authorization for value for content search
    12. sunbird_pg_host: postgres host name or ip
    13. sunbird_pg_port: postgres port number
    14. sunbird_pg_db: postgres db name
    15. sunbird_pg_user: postgres db user name
    16. sunbird_pg_password: postgress db password 
    17. sunbird_installation
    18. sunbird_content_service_api_base_url
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
    33 sunbird_fcm_account_key : account key for FCM 
    34 sunbird_msg_91_auth : msg 91 auth 
    35. sunbird_msg_sender  : message sender name
    36. sunbird_installation_email  : email of admin per installation
    37. sunbird_lms_base_url : sunbird lms service based url
    38. sunbird_lms_authorization : api gateway auth key 
    39 sunbird_mw_system_host=actor-service
    40. sunbird_mw_system_port=
    41.background_actor_provider=remote
    42.api_actor_provider=off
    43.badging_authorization_key=
    44.sunbird_badger_baseurl=http://badger-service:8000
    45.sunbird_remote_req_router_path=
    46.sunbird_remote_bg_req_router_path=
    47.sunbird_api_base_url=http://content-service:
    48.sunbird_authorization={}
    49.telemetry_pdata_id={{env}}.sunbird.learning.service
    50.telemetry_pdata_pid=actor-service
    51.telemetry_pdata_ver=1.5
    
## Do the below env setup , if you are planing to run background actor in remote mode.
	1. sunbird_background_actor_host: host running for userorg background actor
    2. sunbird_background_actor_port: port on which userorg background actor is running.
	
## Do the below env setup , to start a actor system on a machine {"RemoteMiddlewareActorSystem" it will start Normal Actor System on that machine,             ##"BackGroundRemoteMiddlewareActorSystem" , it will start background actor}.

   1.  actor_service_name : actor system name to start{values are "RemoteMiddlewareActorSystem" , "BackGroundRemoteMiddlewareActorSystem"}

## Build
1. Run "git submodule foreach git pull origin master" to pull the latest sunbird-common submodule.
2. Run "mvn clean install -DCLOUD_STORE_GROUP_ID=org.sunbird -DCLOUD_STORE_ARTIFACT_ID=cloud-store-sdk_2.12 -DCLOUD_STORE_VERSION=1.4.7" to build the services.
2. Go to "controller" and run the command "mvn play2:dist" to generate the dist file for controller.
3. The build file "controller-1.0-SNAPSHOT-dist.zip" is generated in "userorg-service/controller/target" folder.

## Run
1. Unzip the dist file "controller-1.0-SNAPSHOT-dist.zip".
2. Run the command "java -cp 'controller-1.0-SNAPSHOT/lib/*' play.core.server.ProdServerStart controller-1.0-SNAPSHOT" to start the service.
