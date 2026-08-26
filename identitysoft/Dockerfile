# ---- Build stage ----
# Note: the -alpine variants of these images currently only publish amd64 (no arm64),
# which would break the build on Apple Silicon / other arm64 dev machines. Using the
# standard Debian-based multi-arch tags instead.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache the dependency layer separately from source changes
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring
COPY --from=build /app/target/*.jar app.jar
USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
