# Build context is the repo root (see infra/docker-compose.yml) so this stage can
# install the shared pantheon-parent BOM before building pantheon-message itself.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY infra/build/pantheon-parent/pom.xml infra/build/pantheon-parent/pom.xml
RUN mvn -f infra/build/pantheon-parent/pom.xml install -q -B

COPY pantheon-message/pom.xml pantheon-message/pom.xml
RUN mvn -f pantheon-message/pom.xml dependency:go-offline -q -B

COPY pantheon-message/src pantheon-message/src
RUN mvn -f pantheon-message/pom.xml package -q -B -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r pantheon && useradd -r -g pantheon pantheon
COPY --from=build /workspace/pantheon-message/target/*.jar app.jar
USER pantheon
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
