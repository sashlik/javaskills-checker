FROM openjdk:11
RUN mkdir /app
WORKDIR /app
COPY javaskills-checker.jar /app/javaskills-checker.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/javaskills-checker.jar"]