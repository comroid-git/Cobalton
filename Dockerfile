
FROM adoptopenjdk/openjdk11:alpine
ARG JAVA_OPTS
ENV JAVA_OPTS=$JAVA_OPTS
ARG COBALTON_OPTS
ENV COBALTON_OPTS=$COBALTON_OPTS
ARG COBALTON_VERSION
ENV COBALTON_VERSION=$COBALTON_VERSION
ADD build/distributions/Cobalton-${COBALTON_VERSION}.tar /opt/bots/

ENTRYPOINT exec /opt/bots/Cobalton-${COBALTON_VERSION}/bin/Cobalton JAVA_OPTS=$JAVA_OPTS COBALTON_OPTS=$COBALTON_OPTS