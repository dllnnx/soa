FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY oscars-service/pom.xml oscars-service/pom.xml
COPY oscars-service/src oscars-service/src
RUN mvn -f oscars-service/pom.xml -DskipTests package

FROM quay.io/wildfly/wildfly:32.0.1.Final-jdk17

COPY --from=build /build/oscars-service/target/oscar.war \
     /opt/jboss/wildfly/standalone/deployments/

EXPOSE 8080
