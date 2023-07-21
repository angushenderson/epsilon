FROM openjdk:17-slim

WORKDIR /app
COPY target/*.jar ./epsilon-core.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar ./epsilon-core.jar"]