## Pre-requisites
1. Akka middleware actors should be up and running. Follow these [instructions](https://github.com/ekstep/sunbird-mw/blob/alpha2/actors/learner-state-mw/setup.md) to run the akka actors.

## Configuration
1. Environment Variabls
    1. sunbird_learnerstate_actor_host
    2. sunbird_learnerstate_actor_port

## Build
1. Run "mvn clean install" from "sunbird-mw/services" to build the services.
2. Go to "sunbird-mw/services/learning-service" and run the command "mvn play2:dist" to generate the dist file for services.
3. The build file "learning-service-1.0-SNAPSHOT-dist.zip" is generated in "sunbird-mw/services/learning-service/target" folder.

## Run
1. Unzip the dist file "learning-service-1.0-SNAPSHOT-dist.zip".
2. Run the command "java -cp 'learning-service-1.0-SNAPSHOT/lib/*' play.core.server.ProdServerStart learning-service-1.0-SNAPSHOT" to start the service.
