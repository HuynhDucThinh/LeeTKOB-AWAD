# Stage 1: Build standalone checker-cli.jar from backend source
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace/backend
COPY backend/gradlew ./gradlew
COPY backend/gradle ./gradle
COPY backend/build.gradle.kts backend/settings.gradle.kts backend/checkstyle.xml ./
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew

COPY backend/src/main ./src/main
RUN ./gradlew --no-daemon checkerCliJar

# Stage 2: Hardened non-root JRE 21 runtime (compatible with --network none --memory=512m --pids-limit=64)
FROM eclipse-temurin:21-jre-jammy

RUN groupadd -g 10001 checker \
    && useradd -u 10001 -g 10001 -m -s /usr/sbin/nologin checker \
    && mkdir -p /app /out \
    && chown -R 10001:10001 /app /out

WORKDIR /app
COPY --from=builder --chown=10001:10001 /workspace/backend/build/libs/checker-cli.jar /app/checker-cli.jar

USER 10001:10001
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-XX:ActiveProcessorCount=2", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/checker-cli.jar"]
