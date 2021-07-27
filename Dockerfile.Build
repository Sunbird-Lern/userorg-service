FROM openjdk:8-jdk-alpine
MAINTAINER "Manojv" "manojv@ilimi.in"
WORKDIR /opt
RUN apk update \
    && mkdir -p /opt/learner \
    && apk add wget \
    && wget http://www-eu.apache.org/dist/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz \
    && tar -xvzf apache-maven-3.3.9-bin.tar.gz
ENV  M2_HOME /opt/apache-maven-3.3.9
ENV  PATH ${M2_HOME}/bin:${PATH}
COPY learner /opt/learner/
WORKDIR /opt/learner/services
RUN mvn clean install -DskipTests
WORKDIR /opt/learner/services/learning-service
CMD ["mvn", "play2:dist"]