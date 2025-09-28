# ---- Build Stage ----
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Maven/Gradle files first (for caching dependencies)
COPY mvnw* pom.xml ./
COPY .mvn .mvn

# Download dependencies (this step gets cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src src

# Build the app
RUN ./mvnw package -DskipTests

# ---- Run Stage ----
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy built jar from previous stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (change if your app uses another port)
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
