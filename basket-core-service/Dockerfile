# Use OpenJDK 21 for basket-core-service
FROM eclipse-temurin:21-jdk

# Set working directory
WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven

# Copy the entire project
COPY . .

# Build the entire project first from the root
RUN mvn clean install -DskipTests -pl basket-core-service -am

# Expose port
EXPOSE 8081

# Set Java options for better performance
ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC"

# Run the application
CMD ["java", "-jar", "basket-core-service/target/basket-core-service-1.0.0-SNAPSHOT.jar"]
