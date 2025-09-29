FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy the built JAR file
COPY sma-adapter-service/target/sma-adapter-service-*.jar app.jar

# Create log directory
RUN mkdir -p /var/log

# Expose port
EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8084/sma-adapter/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
