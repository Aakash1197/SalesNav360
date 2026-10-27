FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl \
    && addgroup -S app \
    && adduser -S app -G app
COPY --from=build /workspace/target/salesnav360-api-*.jar app.jar
USER app
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=5s --retries=5 \
    CMD curl --fail http://localhost:8082/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
