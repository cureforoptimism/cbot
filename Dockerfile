FROM amazoncorretto:17

ADD . /cbot
WORKDIR cbot
RUN ./gradlew bootjar

ENTRYPOINT java -jar build/libs/cbot-0.0.1-SNAPSHOT.jar
