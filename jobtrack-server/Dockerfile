FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21.0.8_9-jre-jammy

RUN groupadd --system jobtrack && useradd --system --gid jobtrack --home-dir /app --shell /usr/sbin/nologin jobtrack
WORKDIR /app
COPY --from=build /workspace/target/jobtrack-server-*.jar /app/app.jar
RUN mkdir -p /app/data/uploads && chown -R jobtrack:jobtrack /app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=25 -Djava.security.egd=file:/dev/./urandom"
ENV TZ=Asia/Shanghai
USER jobtrack
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q 'UP' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
