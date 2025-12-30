# Use the standard JRE 21 image (which supports both x86 and ARM64)
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/user-service-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
