FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy the built JAR file
COPY fix-adapter-service/target/fix-adapter-service-*.jar app.jar

# Create log directory
RUN mkdir -p /var/log/fix

# Expose port
EXPOSE 8085

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8085/fix-adapter/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
