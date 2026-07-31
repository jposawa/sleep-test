FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY build.gradle gradlew settings.gradle ./
COPY gradle/ gradle/

RUN ./gradlew wrapper

COPY src/ src

# assemble, not build: the test task includes integration tests that need the
# PostgreSQL instance from docker-compose, which does not exist while the image
# is being built. Tests are run explicitly - see README.
RUN ./gradlew assemble

ENTRYPOINT ["java","-jar","build/libs/sleep-0.0.1-SNAPSHOT.jar"]
