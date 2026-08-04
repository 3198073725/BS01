# Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# ffmpeg: video transcoding; curl: healthcheck
RUN apk add --no-cache ffmpeg curl \
    && addgroup -S app \
    && adduser -S app -G app \
    && mkdir -p /app/media /app/logs \
    && chown -R app:app /app

COPY --from=build /app/target/*.jar app.jar

USER app
EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=5 \
  CMD curl -fsS --max-time 3 http://localhost:8000/api/health/ || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
