# Windows兼容的JDK17镜像
FROM openjdk:17-jdk-slim

WORKDIR /app

# 复制打包好的jar包（Windows下mvn打包后会生成在target目录）
COPY target/*.jar ai-recruit-system.jar

EXPOSE 8080

# Windows下容器启动命令，避免换行符问题
ENTRYPOINT ["java", "-jar", "ai-recruit-system.jar", "--spring.profiles.active=prod"]