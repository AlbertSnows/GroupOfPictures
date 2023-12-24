FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
WORKDIR /app
COPY . /app
# RUN ./gradlew build
COPY build/libs/*.jar app.jar
ENTRYPOINT ["/bin/bash", "/app/entrypoint.sh"]
