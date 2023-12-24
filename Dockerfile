FROM eclipse-temurin:17-jdk-alpine
EXPOSE 8080 8080
EXPOSE 8000 8000
VOLUME /tmp
WORKDIR /app
COPY . /app
# RUN ./gradlew build
COPY build/libs/*.jar app.jar
ENTRYPOINT ["/bin/bash", "/app/entrypoint.sh"]
