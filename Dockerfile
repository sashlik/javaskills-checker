FROM openjdk:11
RUN mkdir /app
WORKDIR /app
COPY target/javaskills-checker.jar /app/javaskills-checker.jar
EXPOSE 9080
ENTRYPOINT ["java", "-jar", "/app/javaskills-checker.jar"]