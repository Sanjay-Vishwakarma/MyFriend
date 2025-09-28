# ---- Build Stage ----
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw* pom.xml ./
COPY .mvn .mvn

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies (cached)
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src src

# Build the app
RUN ./mvnw package -DskipTests

# ---- Run Stage ----
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
