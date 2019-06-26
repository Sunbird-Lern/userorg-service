FROM openjdk:8-jdk-alpine
MAINTAINER "Manojv" "manojv@ilimi.in"
RUN apk update \
    && apk add unzip \
    && apk add curl \
    && adduser -u 1001 -h /home/sunbird/ -D sunbird \
    && mkdir -p /home/sunbird/learner
COPY ./service/target/actor-service.jar /home/sunbird/learner/
RUN chown -R sunbird:sunbird /home/sunbird
EXPOSE 8088
USER sunbird
WORKDIR /home/sunbird/learner/
CMD ["java",  "-cp", "actor-service.jar", "-Dactor_hostname=actor-service", "-Dbind_hostname=0.0.0.0", "org.sunbird.middleware.Application"]
