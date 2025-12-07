# Simple Docker build for User CRUD Service
FROM gradle:8-jdk17 AS build
COPY . /app
WORKDIR /app
RUN gradle jar --no-daemon

FROM eclipse-temurin:17-jre
COPY --from=build /app/build/libs/*.jar /app/app.jar
WORKDIR /app
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
