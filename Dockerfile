FROM adoptopenjdk/openjdk11:alpine

VOLUME ./build/install/Cobalton /exec
VOLUME /var/bots/Cobalton /data

WORKDIR /

ENTRYPOINT exec /bin/Cobalton
