FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /build
COPY pom.xml ./
COPY oscars-service/pom.xml oscars-service/pom.xml
COPY oscars-service/src oscars-service/src
RUN mvn -f oscars-service/pom.xml clean package

FROM quay.io/wildfly/wildfly:latest-jdk17

COPY --chown=jboss:root --from=build /build/oscars-service/target/oscar.war /opt/jboss/wildfly/standalone/deployments/oscar.war

EXPOSE 8080
