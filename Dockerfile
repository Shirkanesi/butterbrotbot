#
# Build stage
#
FROM maven:3.6.0-jdk-11-slim AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package

#
# Package stage
#
FROM openjdk:11-jre-slim
COPY --from=build /home/app/target/bbbot.jar /usr/local/lib/bbbot.jar
ENTRYPOINT ["java","-jar","/usr/local/lib/bbbot.jar"]

#FROM openjdk:8
#COPY ./data /usr/src/bbbot
#WORKDIR /usr/src/bbbot
#CMD ["java", "-jar", "bbbot.jar"]