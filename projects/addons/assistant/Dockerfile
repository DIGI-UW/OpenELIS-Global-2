# The AI assistant add-on service. Build from this directory:
#   podman build -t openelis-addon-assistant:1.0.0 .
FROM docker.io/library/maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml addon.json ./
RUN mvn -B -ntp -q dependency:go-offline
COPY src src
RUN mvn -B -ntp -q package \
 && jlink --add-modules "$(jdeps --ignore-missing-deps --print-module-deps --multi-release 21 target/assistant.jar),jdk.crypto.ec,jdk.httpserver" \
      --strip-debug --no-header-files --no-man-pages --compress=zip-6 --output /jre

FROM docker.io/library/debian:bookworm-slim
COPY --from=build /jre /opt/jre
COPY --from=build /src/target/assistant.jar /opt/assistant/assistant.jar
ENV PORT=8080 ASSISTANT_DATA_DIR=/data
# the usage log (daily cap, audit) goes to /data; mount a volume there to keep it across restarts
RUN mkdir -p /data && chown 65534:65534 /data
VOLUME /data
USER 65534:65534
EXPOSE 8080
ENTRYPOINT ["/opt/jre/bin/java", "-Xmx96m", "-Xss512k", "-XX:+UseSerialGC", "-XX:+ExitOnOutOfMemoryError", \
            "-jar", "/opt/assistant/assistant.jar"]
