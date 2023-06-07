FROM adoptopenjdk/openjdk11:alpine-slim
MAINTAINER "Manojv" "manojv@ilimi.in"
RUN apk update \
    && apk add  unzip \
    && apk add curl \
    && adduser -u 1001 -h /home/sunbird/ -D sunbird \
    && mkdir -p /home/sunbird
#ENV sunbird_learnerstate_actor_host 52.172.24.203
#ENV sunbird_learnerstate_actor_port 8088 
RUN chown -R sunbird:sunbird /home/sunbird
USER sunbird
COPY ./controller/target/userorg-service-1.0-SNAPSHOT-dist.zip /home/sunbird/
RUN unzip /home/sunbird/userorg-service-1.0-SNAPSHOT-dist.zip -d /home/sunbird/
WORKDIR /home/sunbird/
CMD java -XX:+PrintFlagsFinal $JAVA_OPTIONS -Dplay.server.http.idleTimeout=180s -Dlog4j2.formatMsgNoLookups=true -cp '/home/sunbird/userorg-service-1.0-SNAPSHOT/lib/*' play.core.server.ProdServerStart  /home/sunbird/userorg-service-1.0-SNAPSHOT

