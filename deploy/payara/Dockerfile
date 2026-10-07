FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml moviesApi.yaml ./
COPY movies-service/pom.xml movies-service/pom.xml
RUN mvn -f movies-service/pom.xml -DskipTests dependency:go-offline \
 && mvn -f movies-service/pom.xml -q dependency:get -Dartifact=org.postgresql:postgresql:42.7.4 -Dtransitive=false
COPY movies-service/src movies-service/src
RUN mvn -f movies-service/pom.xml -DskipTests package

FROM payara/server-full:6.2024.12-jdk17

USER root

COPY deploy/certs/out/ca.crt /tmp/ca.crt
COPY --chmod=755 deploy/payara/entrypoint.sh /usr/local/bin/soa-payara-entrypoint

RUN keytool -importcert -noprompt -alias soa-lab-ca -file /tmp/ca.crt \
      -cacerts -storepass changeit \
 && keytool -importcert -noprompt -alias soa-lab-ca -file /tmp/ca.crt \
      -keystore /opt/payara/appserver/glassfish/domains/domain1/config/cacerts.p12 \
      -storepass changeit \
 && chown payara:payara /opt/payara/appserver/glassfish/domains/domain1/config/cacerts.p12 \
 && rm -f /tmp/ca.crt

COPY --from=build /root/.m2/repository/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar \
     /opt/payara/appserver/glassfish/domains/domain1/lib/ext/

# Образ читает исходный post-boot файл из /opt/payara/config.
COPY --chown=1000:1000 deploy/payara/post-boot-commands.asadmin \
     /opt/payara/config/post-boot-commands.asadmin

COPY --from=build --chown=1000:1000 /build/movies-service/target/movies.war /opt/payara/movies.war

ENTRYPOINT ["tini", "--", "/usr/local/bin/soa-payara-entrypoint"]
CMD ["/opt/payara/scripts/entrypoint.sh"]
