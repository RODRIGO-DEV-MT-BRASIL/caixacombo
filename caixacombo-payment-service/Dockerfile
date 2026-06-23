FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiar JAR
COPY target/*.jar app.jar

# Variaveis de ambiente
ENV JAVA_OPTS="-Xms256m -Xmx512m"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
