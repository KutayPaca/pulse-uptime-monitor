# Aşama 1: Derleme (Build)
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
# Bağımlılıkları önceden indirerek önbelleğe al
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Aşama 2: Çalıştırma (Runtime)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]