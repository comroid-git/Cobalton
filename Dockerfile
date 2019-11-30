# Using OpenJDK11
FROM adoptopenjdk/openjdk11:alpine
RUN adduser -h /app -D exec

# Mount Volumes
COPY ./docker /app

# Permission Management
RUN chown -R exec:exec /app/*
RUN chmod -R 755 /app/*
USER exec
WORKDIR /app

RUN ls -AlhX

# GO
ENTRYPOINT exec ./bin/Cobalton
