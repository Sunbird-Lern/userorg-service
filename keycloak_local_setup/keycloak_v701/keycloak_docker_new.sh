mkdir $HOME/sunbird-dbs
export sunbird_dbs_path=$HOME/sunbird-dbs
echo $sunbird_dbs_path

docker network create keycloak-postgres-network
docker run --name=kc_postgres_new \
  --net keycloak-postgres-network \
  -e POSTGRES_PASSWORD=kcpgpassword \
  -e POSTGRES_USER=kcpgadmin \
  -e POSTGRES_DB=quartz_new \
  -e JDBC_PARAMS="useSSL=false" \
  -p 32769:5432 \
  -d postgres:11.2

echo "postgres container created."

mkdir $sunbird_dbs_path/nginx

mkdir $sunbird_dbs_path/nginx/data

mkdir $sunbird_dbs_path/keycloak

mkdir $sunbird_dbs_path/keycloak/tmp

cp -r themes $sunbird_dbs_path/keycloak

cp -r configuration $sunbird_dbs_path/keycloak

cp -r modules $sunbird_dbs_path/keycloak

cp -r realm $sunbird_dbs_path/keycloak

cp -r spi $sunbird_dbs_path/keycloak

# command to disable ubuntu-firewall for ubuntu machines
ufw disable

export docker_network_gateway=$(docker inspect -f '{{range.NetworkSettings.Networks}}{{.Gateway}}{{end}}' kc_postgres_new)

docker run --name kc_local_new -p 8080:8080 \
        -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=sunbird \
        -v $sunbird_dbs_path/keycloak/tmp:/tmp \
        -v $sunbird_dbs_path/keycloak/realm:/opt/jboss/keycloak/imports \
        -v $sunbird_dbs_path/keycloak/spi:/opt/jboss/keycloak/providers \
        -v $sunbird_dbs_path/keycloak/modules:/opt/jboss/keycloak/modules/system/layers/keycloak/org/postgresql/main \
        --net keycloak-postgres-network \
        -e KEYCLOAK_IMPORT="/opt/jboss/keycloak/imports/sunbird-realm.json -Dkeycloak.profile.feature.upload_scripts=enabled" \
        -e sunbird_user_service_base_url="http://localhost:9000" \
        -d jboss/keycloak:7.0.1


echo "keycloak container created."

docker cp themes/sunbird kc_local_new:/opt/jboss/keycloak/themes/sunbird

echo "sunbird themes copied to container."

docker cp configuration/standalone-ha.xml kc_local_new:/opt/jboss/keycloak/standalone/configuration/standalone-ha.xml

echo "sunbird configuration copied to container."

docker container restart kc_local_new

echo "keycloak container restarted after integrating sunbird realm, spi provider and themes."

exit