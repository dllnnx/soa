FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY config-server/pom.xml config-server/pom.xml
RUN mvn -f config-server/pom.xml -DskipTests dependency:go-offline
COPY config-server/src config-server/src
RUN mvn -f config-server/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN groupadd -r app && useradd -r -g app app
USER app
COPY --from=build --chown=app:app /build/config-server/target/config-server.jar /app/config-server.jar
EXPOSE 8888
ENTRYPOINT ["java", "-Xms48m", "-Xmx160m", "-XX:+UseSerialGC", "-jar", "/app/config-server.jar"]
