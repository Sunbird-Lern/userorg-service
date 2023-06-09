mkdir $HOME/sunbird-dbs
export sunbird_dbs_path=$HOME/sunbird-dbs
echo $sunbird_dbs_path

docker network create keycloak-postgres-network
docker run --name=kc_postgres \
  --net keycloak-postgres-network \
  -e POSTGRES_PASSWORD=kcpgpassword \
  -e POSTGRES_USER=kcpgadmin \
  -e POSTGRES_DB=quartz \
  -e JDBC_PARAMS="useSSL=false" \
  -p 32769:5432 \
  -d postgres:11.2

echo "postgres container created."

mkdir $sunbird_dbs_path/nginx

mkdir $sunbird_dbs_path/nginx/data

cp -r nginx $sunbird_dbs_path/nginx/data

sudo docker run --name nginx_local -p 80:80 \
	-v $sunbird_dbs_path/nginx/data/certs:/etc/nginx/certs:ro \
  	-v $sunbird_dbs_path/nginx/data/conf.d:/etc/nginx/conf.d \
  	-v $sunbird_dbs_path/nginx/data/nginx:/etc/nginx \
  	-v $sunbird_dbs_path/nginx/data/vhost.d:/etc/nginx/vhost.d \
  	-v $sunbird_dbs_path/nginx/data/html:/usr/share/nginx/html \
  	-v $sunbird_dbs_path/var/run/docker.sock:/tmp/docker.sock:ro \
  	-d nginx

docker cp $sunbird_dbs_path/nginx/data/nginx.conf nginx_local:/etc/nginx/

echo "nginx container created."

mkdir $sunbird_dbs_path/keycloak

mkdir $sunbird_dbs_path/keycloak/tmp

cp -r themes $sunbird_dbs_path/keycloak

cp -r configuration $sunbird_dbs_path/keycloak

cp -r modules $sunbird_dbs_path/keycloak

cp -r realm $sunbird_dbs_path/keycloak

cp -r spi $sunbird_dbs_path/keycloak

echo "ls $sunbird_dbs_path/keycloak"

docker run --name kc_local -p 8080:8080 \
        -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=sunbird \
        -v $sunbird_dbs_path/keycloak/tmp:/tmp \
        -v $sunbird_dbs_path/keycloak/realm:/opt/jboss/keycloak/imports \
        -v $sunbird_dbs_path/keycloak/spi:/opt/jboss/keycloak/providers \
        -v $sunbird_dbs_path/keycloak/modules:/opt/jboss/keycloak/modules/system/layers/keycloak/org/postgresql/main \
        --net keycloak-postgres-network \
        -e PROXY_ADDRESS_FORWARDING=true \
        -e KEYCLOAK_IMPORT="/opt/jboss/keycloak/imports/sunbird-realm.json -Dkeycloak.profile.feature.upload_scripts=enabled" \
        -d jboss/keycloak:7.0.1


echo "keycloak container created."

docker container restart kc_local

echo "keycloak container restarted."

docker cp themes/sunbird kc_local:/opt/jboss/keycloak/themes/sunbird

echo "sunbird themes copied to container."

docker cp configuration/standalone-ha.xml kc_local:/opt/jboss/keycloak/standalone/configuration/standalone-ha.xml

echo "sunbird configuration copied to container."

docker container restart kc_local

echo "keycloak container restarted after integrating sunbird realm, spi provider and themes."

exit