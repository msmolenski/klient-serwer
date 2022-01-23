FROM openjdk:latest
ADD build\libs\Currency-rate-0.0.1-SNAPSHOT.jar .
EXPOSE 8000
CMD java -jar Currency-rate-0.0.1-SNAPSHOT.jar