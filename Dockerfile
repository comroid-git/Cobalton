# Using OpenJDK11
FROM adoptopenjdk/openjdk11:alpine
RUN adduser -h /app -D exec

# Permission Management
USER exec
WORKDIR /app

RUN ls -AlhX

# GO
ENTRYPOINT /app/bin/Cobalton
