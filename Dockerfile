FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

ARG GITHUB_ACTOR
ARG GITHUB_TOKEN

RUN mkdir -p /root/.m2
RUN echo '<settings><servers><server><id>github</id><username>'$GITHUB_ACTOR'</username><password>'$GITHUB_TOKEN'</password></server></servers></settings>' > /root/.m2/settings.xml

COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]