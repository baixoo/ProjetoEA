FROM eclipse-temurin:25-jdk-alpine AS dev

WORKDIR /app

# Copy Maven wrapper + config
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

RUN chmod +x mvnw

## Dependencies are cached
RUN ./mvnw dependency:go-offline

# Copy source
COPY src src

# Build
RUN ./mvnw clean package -DskipTests

# Run app
EXPOSE 8080

CMD java -jar target/*.jar
