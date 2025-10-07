# syntax=docker/dockerfile:1

# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Optimize dependency download
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests dependency:go-offline

# Copy sources and build
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    if [ ! -d "src" ]; then echo "ERROR: src directory not found in build context"; ls -la; exit 1; fi && \
    mvn -q -DskipTests package && \
    JAR=$(ls target/*.jar | grep -v original | head -n 1) && \
    cp "$JAR" app.jar

# Runtime stage
FROM openjdk:21-jdk-slim
WORKDIR /app

# Copy the built jar from builder stage (finalName is set to 'app' in pom.xml)
COPY --from=builder /workspace/app.jar app.jar

# Expose Spring Boot configured port
EXPOSE 8086

# Optionally pass JVM args via JAVA_OPTS env var (set in docker-compose if needed)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
