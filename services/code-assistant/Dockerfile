# Use OpenJDK 17 as the base image
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the JAR file
COPY target/code-assistant-api-1.0.0.jar app.jar

# Expose port 8080
EXPOSE 8080

# Set environment variables for Google Cloud Run
ENV PORT=8080
ENV SERVER_PORT=8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"] 