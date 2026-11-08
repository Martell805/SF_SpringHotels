FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
ARG MODULE
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests -pl ${MODULE} -am package

FROM eclipse-temurin:17-jre
ARG MODULE
WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/${MODULE}-1.0.0.jar app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
