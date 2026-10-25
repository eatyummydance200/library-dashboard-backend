FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
COPY schema.sql seed.json ./
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
LABEL org.opencontainers.image.source="https://github.com/eatyummydance200/library-dashboard-backend"
WORKDIR /app
RUN useradd --uid 10001 --create-home app && mkdir /app/data && chown app:app /app/data
COPY --from=build /build/target/app.jar ./app.jar
USER app
EXPOSE 8000
ENV JAVA_TOOL_OPTIONS="-Xms32m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=64m"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
