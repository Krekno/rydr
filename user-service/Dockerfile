############################
# Stage 1 — Build
############################
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew

RUN ./gradlew dependencies --no-daemon --console=plain

COPY src ./src
RUN ./gradlew bootJar --no-daemon --console=plain

############################
# Stage 2 — Runtime
############################
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app

COPY --from=build --chown=app:app /app/build/libs/*.jar app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]