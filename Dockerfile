# syntax=docker/dockerfile:experimental
# https://spring.io/guides/topicals/spring-boot-docker/
# AS build allows us to reference this context further in the file
# less frequent at the top, more frequent changes at the bottom
FROM eclipse-temurin:17-jdk AS build


# set work space up
WORKDIR /workspace/app
COPY . /workspace/app
RUN chmod +x gradlew

# When doing a gradle clean build, we don't generally want to remake
# /root/.gradle so this says to not rebuild it unless it changes
RUN --mount=type=cache,target=/root/.gradle ./gradlew clean build

# For jar -xf ../libs/*-SNAPSHOT.jar
# we are doing an (x)traction of (f)ile ../libs/*-snapshot.jar
# which is the jar spring boot builds
RUN mkdir -p build/dependency && (cd build/dependency; jar -xf ../libs/*-SNAPSHOT.jar)


FROM eclipse-temurin:17-jdk
# volume specifices where external(persistent) data is to be stored
VOLUME /tmp
RUN apt-get update && apt-get install -y ffmpeg && apt-get clean
# all files we want to grab will be under this directory from build
ARG DEPENDENCY=/workspace/app/build/dependency
# copy those files from build, and put them in these corresponding folders
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
COPY --from=build /workspace/app/build/resources build/resources

#WORKDIR /workspace/app
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh
# RUN /workspace/app/entrypoint.sh
#ENTRYPOINT ["entrypoint.sh"]
# Update package lists and install FFmpeg


ENTRYPOINT ["/bin/bash", "/app/entrypoint.sh"]