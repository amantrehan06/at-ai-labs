# Multi-stage build for AT AI Labs
FROM maven:3.9.4-openjdk-17 AS builder

# Set working directory
WORKDIR /app

# Copy the entire project
COPY . .

# Build the application
RUN mvn clean package -DskipTests -pl common -am

# Runtime stage
FROM openjdk:17-jre-slim

# Set working directory
WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/common/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080

# Run the application
CMD ["java", "-jar", "app.jar"] 