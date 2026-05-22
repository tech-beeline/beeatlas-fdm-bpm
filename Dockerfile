FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends maven && \
    rm -rf /var/lib/apt/lists/*

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && \
    useradd -m -d /app -s /bin/bash -u 1000 appuser && \
    chown -R appuser:appuser /app

COPY --from=build --chown=appuser:appuser /app/target/fdm-bpm-*.jar app.jar

USER appuser
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=90s --retries=12 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
