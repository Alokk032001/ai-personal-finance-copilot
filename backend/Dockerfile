# Stage 1: Build the jar using Maven on JDK 25
FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Create lightweight runtime container with JDK/JRE 25
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]