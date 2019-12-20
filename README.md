# sunbird-lms-service
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b963e5ed122f47b5a27b19a87d9fa6de)](https://app.codacy.com/app/sunbird-bot/sunbird-lms-service?utm_source=github.com&utm_medium=referral&utm_content=project-sunbird/sunbird-lms-service&utm_campaign=Badge_Grade_Settings)

This is the repository for Sunbird learning management system (lms) micro-service. It provides the APIs for lms functionality of Sunbird.

The code in this repository is licensed under MIT License unless otherwise noted. Please see the [LICENSE](https://github.com/project-sunbird/sunbird-lms-service/blob/master/LICENSE) file for details.

# Steps to run service on developer machine
1. clone sunbird-util repo : https://github.com/project-sunbird/sunbird-utils
2. clone sunbird-lms-mw repo : https://github.com/project-sunbird/sunbird-lms-mw
3. clone sunbird-lms-service repo : https://github.com/project-sunbird/sunbird-lms-service
4. install cassandra version-3.11.5
5. install Elasticsearch-6.3.0
6. run cassandra.cql form here : https://github.com/project-sunbird/sunbird-lms-mw/blob/master/service/src/main/resources/cassandra.cql (During run there might be some error , developer can ignore that)
7. run cassandra migration job from here: https://github.com/project-sunbird/sunbird-utils/tree/master/sunbird-cassandra-migration/cassandra-migration 
   -- Env required to run cassandra migration job:
     "sunbird_cassandra_port" : cassandra port number
	   "sunbird_cassandra_host"; : list of host comma separatd Example "127.0.0.1,127.0.0.2" or "127.0.0.1" in case on only one host 
      "sunbird_cassandra_username"; : optional if it has userName
	    "sunbird_cassandra_password"; : optioan if it has password
	    "sunbird_cassandra_keyspace"; : provide the keyspace name against which you want to run migration.
	 // key space should be created before runing job.
  
     // run maven with command :
      mvn clean install -DskipTests
      mvn exec:java
  Note: if any cassandra migration failed it will show the file number and error mesage (either fix that or manually go to cassandra_migration_version table and make success column value from false to True)    
8. create elasticsearch index and mapping from here: https://github.com/project-sunbird/sunbird-utils/tree/master/sunbird-es-utils/src/main/resources  (for more details use: https://project-sunbird.atlassian.net/wiki/spaces/SBDES/pages/1030094992/Elasticsearch+mapping+update+job+steps)
9. Set custodian org details inside system settings : http://docs.sunbird.org/latest/developer-docs/server-configurations/system_settings/
10. compile sunbird-util
11. compile sunbird-lms-mw (in pom.xml comment <!-- <<module>actors/sunbird-utils</module> -->)
12. compile sunbird-lms-service (in pom.xml comment <!-- <module>actors/sunbird-lms-mw</module> -->)
13. set env (http://docs.sunbird.org/latest/developer-docs/server-configurations/env_variables_lms/)
14. install keyclaok 3.2.0 
15. run sunbird-lms-service as follow (change service directory and run mvn play2:run)

