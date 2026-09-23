# syntax=docker/dockerfile:1
# ============================
# 1) Dependencies Stage
# ============================
FROM gradle:8.12.1-jdk21 AS dependencies
WORKDIR /app

COPY build.gradle settings.gradle gradle.properties* gradlew gradlew.bat ./
COPY gradle/ ./gradle/

RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    chmod +x ./gradlew && \
    (./gradlew dependencies --no-daemon || gradle dependencies --no-daemon)

# ============================
# 2) Build Stage
# ============================
FROM dependencies AS build

COPY src/ ./src/

RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    ./gradlew clean build -x test --no-daemon

# ============================
# 3) Runtime Stage
# ============================
ARG DSP_BACKEND_PROJECT_NAME=rer-dsp-backend
ARG DSP_BACKEND_VERSION=0.0.1-SNAPSHOT

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

ARG DSP_BACKEND_PROJECT_NAME=rer-dsp-backend
ARG DSP_BACKEND_VERSION=0.0.1-SNAPSHOT

RUN apt-get update && apt-get upgrade -y \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

COPY --from=build /app/build/libs/${DSP_BACKEND_PROJECT_NAME}-${DSP_BACKEND_VERSION}.jar /app/app.jar

# Extra dsp_config context (rer-dsp-core/config) injected by Compose.
COPY --from=dsp_config docker/select-runtime-config.sh /tmp/select-runtime-config.sh
COPY --from=dsp_config installation/ /tmp/installation/
COPY --from=dsp_config map/ /tmp/map/
COPY --from=dsp_config downloads/ /tmp/downloads/
COPY --from=dsp_config about/ /tmp/about/
RUN chmod +x /tmp/select-runtime-config.sh \
    && mkdir -p /config \
    && /tmp/select-runtime-config.sh pick /tmp/installation installation-config.json /config/installation-config.json \
    && /tmp/select-runtime-config.sh pick /tmp/map mapLayersConfig.json /config/mapLayersConfig.json \
    && /tmp/select-runtime-config.sh pick /tmp/downloads downloadThemesConfig.json /config/downloadThemesConfig.json \
    && /tmp/select-runtime-config.sh about /tmp/about /config/about \
    && rm -rf /tmp/installation /tmp/map /tmp/downloads /tmp/about /tmp/select-runtime-config.sh

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
