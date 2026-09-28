FROM maven:3.9.11-eclipse-temurin-25 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre

WORKDIR /app
RUN groupadd --system peladinhas \
    && useradd --system --gid peladinhas --home-dir /app --shell /usr/sbin/nologin peladinhas
COPY --from=build --chown=peladinhas:peladinhas /workspace/target/peladinhas-backend-0.1.0-SNAPSHOT.jar /app/peladinhas-backend.jar
USER peladinhas
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/peladinhas-backend.jar"]
