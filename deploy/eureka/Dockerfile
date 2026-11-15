FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY eureka-server/pom.xml eureka-server/pom.xml
RUN mvn -f eureka-server/pom.xml -DskipTests dependency:go-offline
COPY eureka-server/src eureka-server/src
RUN mvn -f eureka-server/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN groupadd -r app && useradd -r -g app app
USER app
COPY --from=build --chown=app:app /build/eureka-server/target/eureka-server.jar /app/eureka-server.jar
EXPOSE 8761
ENTRYPOINT ["java", "-Xms48m", "-Xmx192m", "-XX:+UseSerialGC", "-jar", "/app/eureka-server.jar"]
