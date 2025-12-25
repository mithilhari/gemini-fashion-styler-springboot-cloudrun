# -------- BUILD STAGE --------
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy pom first for dependency caching
COPY pom.xml .

RUN mvn -B -q dependency:go-offline

# Copy source
COPY src ./src

# Build
RUN mvn -B -q package -DskipTests


# -------- RUNTIME STAGE --------
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]

