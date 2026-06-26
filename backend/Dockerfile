FROM eclipse-temurin:17-jre
WORKDIR /app
COPY ai-interview-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

# 使用方法：
#   docker build -t ai-interview:latest .
#   docker run -d --name ai-interview --network junle -p 8080:8080 ai-interview:latest
