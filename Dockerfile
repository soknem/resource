# --- STAGE 1: Build Stage ---
# Use Gradle to compile the code and build the JAR
FROM gradle:8.14-jdk21-alpine AS builder
WORKDIR /app

# Copy the project files
COPY . .

# Build the executable JAR, skipping tests to save time
# --no-daemon ensures the process stops after finishing
RUN gradle build --no-daemon -x test

# --- STAGE 2: Run Stage ---
# Use eclipse-temurin JRE (Runtime only) to keep the image small
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar from the builder stage
# Note: Gradle usually builds two jars. We use a wildcard but ensure
# we grab the one that isn't the '-plain.jar' if possible.
COPY --from=builder /app/build/libs/*.jar app.jar

# Open port 8080
EXPOSE 8081

# Environment variable for Spring Profile
ENV SPRING_PROFILES_ACTIVE=prod

# Volumes for persistent datadoc (matching your docker-compose)
VOLUME /home/media
VOLUME /keys

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]