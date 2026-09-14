# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml
COPY pom.xml ./

# Resolve dependencies
RUN mvn dependency:go-offline

# Copy source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create a non-root user and group
RUN addgroup --system javauser && adduser --system --ingroup javauser javauser

# Copy the generated Spring Boot JAR
COPY --from=build /app/target/platform-1.0.0.jar ./platform-1.0.0.jar

# Set ownership of the application file to the non-root user
RUN chown javauser:javauser platform-1.0.0.jar

# Switch to the non-root user
USER javauser

# Expose port 8080
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "platform-1.0.0.jar"]
