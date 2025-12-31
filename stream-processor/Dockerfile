FROM eclipse-temurin:17-jre
LABEL maintainer="backend@wiki-kafka.local"

WORKDIR /app

RUN mkdir -p /tmp/kafka-streams

COPY target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-Xmx1024m", "-Xms512m", "-jar", "app.jar"]