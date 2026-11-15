FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY oscars-service/pom.xml oscars-service/pom.xml
COPY oscars-service/src oscars-service/src
RUN mvn -f oscars-service/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-jammy
USER root

COPY deploy/certs/out/ca.crt /tmp/ca.crt

RUN keytool -importcert -noprompt -alias soa-lab-ca -file /tmp/ca.crt \
      -cacerts -storepass changeit \
 && rm -f /tmp/ca.crt \
 && groupadd -r app && useradd -r -g app app

USER app
COPY --from=build --chown=app:app /build/oscars-service/target/oscar.jar /app/oscar.jar
EXPOSE 8443
ENTRYPOINT ["java", "-Xms64m", "-Xmx256m", "-XX:+UseSerialGC", "-jar", "/app/oscar.jar"]
