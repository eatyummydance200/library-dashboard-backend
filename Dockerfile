FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /build
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY src ./src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre-jammy
LABEL org.opencontainers.image.source="https://github.com/eatyummydance200/library-dashboard-backend"
WORKDIR /app
RUN useradd --uid 10001 --create-home app
COPY --from=build --chown=10001:10001 /build/build/libs/app.jar ./app.jar
USER 10001:10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-Xms32m -XX:MaxRAMPercentage=50"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
