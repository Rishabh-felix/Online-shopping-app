
# base image
FROM openjdk:17
# copying the jar files from the target folder and then naming it to app.jar
COPY target/*.jar app.jar
# to point to this jar file
ENTRYPOINT ["java","-jar","/app.jar"]