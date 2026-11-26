# Build the Spring Boot API from the Maven multi-module repository.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY csme-shared/pom.xml csme-shared/pom.xml
COPY csme-server/pom.xml csme-server/pom.xml
COPY csme-desktop/pom.xml csme-desktop/pom.xml
RUN mvn -B -N dependency:go-offline
COPY csme-shared/src csme-shared/src
COPY csme-server/src csme-server/src
RUN mvn -B -pl csme-server -am package -DskipTests

# Small Java 21 runtime image.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/csme-server/target/csme-server.jar /app/app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=75.0 -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
