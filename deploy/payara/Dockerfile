FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml moviesApi.yaml ./
COPY movies-service/pom.xml movies-service/pom.xml
COPY movies-service/src movies-service/src
RUN mvn -f movies-service/pom.xml -DskipTests package \
 && mvn -q dependency:get -Dartifact=org.postgresql:postgresql:42.7.4 -Dtransitive=false

FROM payara/server-full:6.2024.12

COPY --from=build /root/.m2/repository/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar \
     /opt/payara/glassfish/lib/ext/

COPY deploy/payara/post-boot-commands.asadmin \
     /opt/payara/scripts/post-boot-commands.asadmin

COPY --from=build /build/movies-service/target/movies.war /opt/payara/movies.war
