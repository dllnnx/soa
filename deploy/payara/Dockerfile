FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml moviesApi.yaml ./
COPY movies-service/pom.xml movies-service/pom.xml
RUN mvn -f movies-service/pom.xml -DskipTests dependency:go-offline \
 && mvn -f movies-service/pom.xml -q dependency:get -Dartifact=org.postgresql:postgresql:42.7.4 -Dtransitive=false
COPY movies-service/src movies-service/src
RUN mvn -f movies-service/pom.xml -DskipTests package

FROM payara/server-full:6.2024.12-jdk17

COPY --from=build /root/.m2/repository/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar \
     /opt/payara/appserver/glassfish/domains/domain1/lib/ext/

# PKCS#12 с alias s1as, пароль changeit.
COPY --chown=1000:1000 deploy/certs/out/payara.p12 \
     /opt/payara/appserver/glassfish/domains/domain1/config/keystore.p12

# Образ читает исходный post-boot файл из /opt/payara/config.
COPY --chown=1000:1000 deploy/payara/post-boot-commands.asadmin \
     /opt/payara/config/post-boot-commands.asadmin

COPY --from=build --chown=1000:1000 /build/movies-service/target/movies.war /opt/payara/movies.war
