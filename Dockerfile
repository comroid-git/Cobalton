FROM adoptopenjdk/openjdk11:alpine

ARG JAVA_OPTS
ENV JAVA_OPTS=$JAVA_OPTS
ARG COBALTON_OPTS
ENV COBALTON_OPTS=$COBALTON_OPTS

COPY build/install/Cobalton/bin /bin
COPY build/install/Cobalton/lib /lib

VOLUME /var/bots/Cobalton /data

ENTRYPOINT exec /bin/Cobalton JAVA_OPTS=$JAVA_OPTS COBALTON_OPTS=$COBALTON_OPTS
