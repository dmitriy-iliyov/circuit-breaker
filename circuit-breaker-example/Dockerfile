FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# build lib
COPY pom.xml pom.xml
COPY circuit-breaker-core circuit-breaker-core
COPY circuit-breaker-aop circuit-breaker-aop
COPY circuit-breaker-starter circuit-breaker-starter

RUN mvn clean install -Dgpg.skip=true -DskipTests

# build example
COPY . .
RUN mvn -f circuit-breaker-example/pom.xml clean install -DskipTests

FROM eclipse-temurin:21-jre AS final
WORKDIR /app
COPY --from=builder /app/circuit-breaker-example/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]