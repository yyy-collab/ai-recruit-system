# 使用 JDK 17 基础镜像（轻量版）
FROM openjdk:17-jdk-slim

# 设置时区为上海
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 设置工作目录
WORKDIR /app

# 复制 jar 包到容器内
COPY target/ai-recruit-system-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]