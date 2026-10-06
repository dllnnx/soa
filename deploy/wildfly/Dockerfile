FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY oscars-service/pom.xml oscars-service/pom.xml
COPY oscars-service/src oscars-service/src
RUN mvn -f oscars-service/pom.xml -DskipTests package

FROM quay.io/wildfly/wildfly:32.0.1.Final-jdk17

COPY --chown=jboss:root deploy/certs/out/wildfly.p12 /opt/jboss/wildfly/standalone/configuration/wildfly.p12
COPY deploy/certs/out/ca.crt /tmp/ca.crt
COPY deploy/wildfly/configure.cli /tmp/configure.cli

RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/tmp/configure.cli \
 && rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history

USER root
RUN keytool -importcert -noprompt -alias soa-lab-ca -file /tmp/ca.crt \
      -cacerts -storepass changeit \
 && rm -f /tmp/ca.crt /tmp/configure.cli
USER jboss

COPY --from=build /build/oscars-service/target/oscar.war \
     /opt/jboss/wildfly/standalone/deployments/

EXPOSE 8443
