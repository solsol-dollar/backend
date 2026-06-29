FROM gradle:8.10-jdk21 AS builder
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew :service-app:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone
ENV TZ=Asia/Seoul
WORKDIR /app
COPY --from=builder /app/service-app/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
