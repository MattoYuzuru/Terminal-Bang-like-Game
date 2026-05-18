FROM gradle:9.4.1-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle --no-daemon clean installDist

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/install/terminal-western-card-game/ ./
EXPOSE 2222
ENV TB_SSH_PORT=2222
ENTRYPOINT ["./bin/terminal-western-card-game"]
