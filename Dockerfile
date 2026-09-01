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

# Production memory profile for a 512 MiB container. Keep the container limit
# aligned with MaxRAM below (or override JAVA_TOOL_OPTIONS at deployment time).
# Serial GC has the lowest native overhead for this single-process service.
ENV JAVA_TOOL_OPTIONS="-XX:+UseSerialGC \
    -XX:MaxRAM=512m \
    -Xms32m \
    -Xmx192m \
    -Xss512k \
    -XX:ReservedCodeCacheSize=32m \
    -XX:MaxDirectMemorySize=32m \
    -XX:ActiveProcessorCount=2 \
    -XX:+UseCompactObjectHeaders \
    -XX:+ExitOnOutOfMemoryError \
    -Dspring.jmx.enabled=false \
    -Dspring.jpa.open-in-view=false \
    -Dserver.tomcat.threads.max=50 \
    -Dserver.tomcat.threads.min-spare=4 \
    -Dserver.tomcat.accept-count=50 \
    -Dspring.datasource.hikari.maximum-pool-size=10 \
    -Dspring.datasource.hikari.minimum-idle=2"

# OpenAPI is not needed by production clients and its controller/DTO scan costs
# memory. Enable explicitly at deployment time if production docs are needed.
ENV SPRINGDOC_API_DOCS_ENABLED=false \
    SPRINGDOC_SWAGGER_UI_ENABLED=false

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
