FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY gateway/pom.xml gateway/pom.xml
RUN mvn -f gateway/pom.xml -DskipTests dependency:go-offline
COPY gateway/src gateway/src
RUN mvn -f gateway/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-jammy
USER root

COPY deploy/certs/out/ca.crt /tmp/ca.crt

RUN keytool -importcert -noprompt -alias soa-lab-ca -file /tmp/ca.crt \
      -cacerts -storepass changeit \
 && rm -f /tmp/ca.crt \
 && groupadd -r app && useradd -r -g app app

USER app
COPY --from=build --chown=app:app /build/gateway/target/gateway.jar /app/gateway.jar
EXPOSE 8443
ENTRYPOINT ["java", "-Xms48m", "-Xmx192m", "-XX:+UseSerialGC", "-jar", "/app/gateway.jar"]
