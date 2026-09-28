FROM eclipse-temurin:17-jre

WORKDIR /app

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY target/devops-pipeline-1.0.5-SNAPSHOT.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

ENTRYPOINT ["java","-jar","app.jar"]
