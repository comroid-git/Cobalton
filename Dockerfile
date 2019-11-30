FROM adoptopenjdk/openjdk11:alpine

VOLUME ./docker /app

WORKDIR /app

ENTRYPOINT exec ./bin/Cobalton
