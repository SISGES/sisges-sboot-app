FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /app/target/*.jar app.jar
USER spring:spring

# Low-memory defaults for local development. These also reduce Spring's pools
# and eager initialization, which has a larger RSS impact than heap sizing alone.
# Override JAVA_TOOL_OPTIONS from docker-compose when debugging or load testing.
ENV JAVA_TOOL_OPTIONS="-XX:+UseSerialGC \
    -Xms16m \
    -Xmx96m \
    -Xss512k \
    -XX:ReservedCodeCacheSize=24m \
    -XX:MaxDirectMemorySize=16m \
    -XX:MinHeapFreeRatio=5 \
    -XX:MaxHeapFreeRatio=15 \
    -XX:ActiveProcessorCount=1 \
    -XX:TieredStopAtLevel=1 \
    -XX:+ExitOnOutOfMemoryError \
    -Dspring.main.lazy-initialization=true \
    -Dspring.jmx.enabled=false \
    -Dspring.jpa.open-in-view=false \
    -Dserver.tomcat.threads.max=20 \
    -Dserver.tomcat.threads.min-spare=2 \
    -Dspring.datasource.hikari.maximum-pool-size=4 \
    -Dspring.datasource.hikari.minimum-idle=1"

# OpenAPI scans every controller and DTO and costs tens of MiB. Keep it off in
# the low-memory image; either variable can be set to true in docker-compose.
ENV SPRINGDOC_API_DOCS_ENABLED=false \
    SPRINGDOC_SWAGGER_UI_ENABLED=false

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
