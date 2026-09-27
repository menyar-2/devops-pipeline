FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/devops-pipeline-1.0.5-SNAPSHOT.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]
