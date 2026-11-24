# Optional backend image (architecture §13). The project runs without Docker (ADR-007).
# Build: docker build -t todo-manager-backend ./backend
# Run:   supply MYSQL_* and OAuth variables at run time; the image contains no secrets.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /src/target/*.jar app.jar
USER app
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
