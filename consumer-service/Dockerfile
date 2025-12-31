FROM eclipse-temurin:17-jre
LABEL maintainer="backend@wiki-kafka.local"

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]