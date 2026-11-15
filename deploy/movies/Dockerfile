FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml moviesApi.yaml ./
COPY movies-service/pom.xml movies-service/pom.xml
RUN mvn -f movies-service/pom.xml -DskipTests dependency:go-offline
COPY movies-service/src movies-service/src
RUN mvn -f movies-service/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN groupadd -r app && useradd -r -g app app
USER app
COPY --from=build --chown=app:app /build/movies-service/target/movies.jar /app/movies.jar
EXPOSE 8181
ENTRYPOINT ["java", "-Xms64m", "-Xmx256m", "-XX:+UseSerialGC", "-jar", "/app/movies.jar"]
