# ====== Build stage ======
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy Maven descriptor and resolve dependencies first for better layer caching
COPY pom.xml .
RUN mvn -B -e -U dependency:go-offline -DskipTests

# Now copy the rest of the source and build
COPY src ./src
RUN mvn -B -e generate-sources package -DskipTests

# ====== Runtime stage ======
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Copy the fat jar built in the previous stage
# Adjust the jar file name if yours differs
COPY --from=build /app/target/*.jar app.jar

# Use a non-root user if desired (UID can differ)
# RUN adduser --disabled-password --gecos "" appuser && chown appuser:appuser app.jar
# USER appuser

EXPOSE 8080
ENV JAVA_OPTS=""

# Spring Boot jar start
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
