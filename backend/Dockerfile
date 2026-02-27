FROM gradle:8.7-jdk21 AS build
WORKDIR /app
COPY build.gradle.kts gradle.properties settings.gradle.kts* ./
COPY src ./src
RUN gradle quarkusBuild --no-daemon -Dquarkus.package.type=uber-jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/quarkus-app/quarkus-run.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
