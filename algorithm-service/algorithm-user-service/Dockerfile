FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

COPY algorithm-service/algorithm-user-service/target/algorithm-user-service-*.jar app.jar

ENV SPRING_PROFILES_ACTIVE="prod"
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
