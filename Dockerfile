FROM maven:3.9-eclipse-temurin-21 AS build
ARG GITHUB_USERNAME
ARG GITHUB_TOKEN
RUN mkdir -p ~/.m2 && echo "<settings><servers><server><id>github</id><username>${GITHUB_USERNAME}</username><password>${GITHUB_TOKEN}</password></server></servers></settings>" > ~/.m2/settings.xml
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]