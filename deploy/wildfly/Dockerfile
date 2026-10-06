# Контекст сборки — корень репозитория:
#   docker build -f deploy/wildfly/Dockerfile .
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY oscars-service/pom.xml oscars-service/pom.xml
COPY oscars-service/src oscars-service/src
RUN mvn -f oscars-service/pom.xml -DskipTests package

FROM quay.io/wildfly/wildfly:32.0.1.Final-jdk17

# Сертификат wildfly (PKCS12, пароль changeit) + корень нашего CA
COPY deploy/certs/out/wildfly.p12 /opt/jboss/wildfly/standalone/configuration/wildfly.p12
COPY deploy/certs/out/ca.crt /tmp/ca.crt
COPY deploy/wildfly/configure.cli /tmp/configure.cli

# HTTPS-listener вместо HTTP; корень CA в truststore JVM,
# чтобы MoviesClient доверял https://payara:8181
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/tmp/configure.cli \
 && keytool -importcert -noprompt -alias soa-lab-ca -file /tmp/ca.crt \
      -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit \
 && rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history \
           /tmp/configure.cli /tmp/ca.crt

COPY --from=build /build/oscars-service/target/oscar.war \
     /opt/jboss/wildfly/standalone/deployments/

EXPOSE 8443
