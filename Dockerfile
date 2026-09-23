FROM maven:3.9.11-eclipse-temurin-25-alpine AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn --batch-mode dependency:go-offline
COPY src src
RUN mvn --batch-mode verify

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S -G app -u 10001 app && mkdir /data && chown app:app /data
WORKDIR /app
COPY --from=build /workspace/target/schwimmkurs-ft-*.jar app.jar
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
