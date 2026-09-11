# ==========================================
# STAGE 1: Build JAR file
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven Wrapper and POM first to leverage the Docker cache layer for dependencies
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Preload dependencies
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code and package JAR (skip tests during image build)
COPY src src
RUN ./mvnw clean package -DskipTests

# ==========================================
# STAGE 2: Runtime Image
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user to enhance security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the JAR file from STAGE 1
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Run the application with optimized JVM configuration
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
