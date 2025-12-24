# Use official Java 21 runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy Maven build
COPY target/*.jar app.jar

# Cloud Run uses PORT env var
ENV PORT=8080

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]

