# ---------- Fase 1: build (Maven) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---------- Fase 2: execução ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Limites de memória para o plano grátis do Render (512 MB de RAM)
ENV JAVA_TOOL_OPTIONS="-Xmx300m -XX:MaxMetaspaceSize=200m -XX:ReservedCodeCacheSize=48m -Xss512k -XX:+UseSerialGC"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]