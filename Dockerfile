FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/msgbridge-0.1.0-SNAPSHOT.jar /app/msgbridge.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/msgbridge.jar"]
