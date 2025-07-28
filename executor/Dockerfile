# Build stage
FROM maven:3.8.4-openjdk-17 AS builder

WORKDIR /app

# Copy the entire project (from root)
COPY . .

# Build the application from root
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jre-slim

WORKDIR /app

# Copy the built JAR from the executor module
COPY --from=builder /app/executor/target/executor-1.0.0.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"] 