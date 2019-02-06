FROM openjdk:8-jre-alpine
MAINTAINER "Manojv" "manojv@ilimi.in"
RUN apk update \
    && apk add  unzip \
    && apk add curl \
    && adduser -u 1001 -h /home/sunbird/ -D sunbird \
    && mkdir -p /home/sunbird/learner
#ENV sunbird_learnerstate_actor_host 52.172.24.203
#ENV sunbird_learnerstate_actor_port 8088 
RUN chown -R sunbird:sunbird /home/sunbird
USER sunbird
COPY ./service/target/learning-service-1.0-SNAPSHOT-dist.zip /home/sunbird/learner/
RUN unzip /home/sunbird/learner/learning-service-1.0-SNAPSHOT-dist.zip -d /home/sunbird/learner/
WORKDIR /home/sunbird/learner/
CMD java  -cp '/home/sunbird/learner/learning-service-1.0-SNAPSHOT/lib/*' play.core.server.ProdServerStart  /home/sunbird/learner/learning-service-1.0-SNAPSHOT
