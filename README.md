# ai-recruit-system
AI招聘猎头系统-团队使用

后端 README（Spring Boot ）
# AI 招聘猎头系统 - 后端

## 项目简介
基于 Spring Boot + MyBatis 的 AI 招聘系统后端，提供求职者/HR 注册登录、岗位管理、简历解析（Apache POI + HanLP）、匹配度计算（TF-IDF + 余弦相似度）等功能。

**技术栈**
- Spring Boot 4.0.6
- MyBatis 4.0.1
- MySQL 8.0.33
- JWT (JJWT)
- BCrypt (jBCrypt)
- Apache POI 5.2.5
- HanLP portable-1.8.4
- PageHelper 1.4.7

## 环境要求
- JDK 11 或 17
- Maven 3.6+
- MySQL 8.0
- IDE: IntelliJ IDEA（推荐）

## 快速开始


### 1. 克隆代码
    ```bash
    git clone https://github.com/yyy-collab/ai-recruit-system-backend.git
    cd ai-recruit-system-backend

### 2.创建数据库并初始化
    ```sql
    CREATE DATABASE ai_recruit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 执行项目中的 docs/database.sql 建表（已提供完整建表语句）


### 3. 修改配置
    编辑 src/main/resources/application.yml：
    yaml
    spring:
      datasource:
        url: jdbc:mysql://localhost:3306/recruit_ai?useSSL=false&serverTimezone=Asia/Shanghai
        username: root
        password: 你的密码
    file:
      upload-dir: D:/upload/   # 文件上传目录，改为实际路径
### 4. 启动项目
    ```bash
    mvn clean install
    mvn spring-boot:run
默认启动端口 8080

### 5. 测试连通性
bash
curl http://localhost:8080/test/ping
应返回 {"code":0,"msg":"操作成功","data":"pong"}

### 6.项目结构
src/main/java/com.recruit.ai_recruitsystem
├── config          # CORS、拦截器、静态资源映射
├── controller      # 控制器（seeker/hr/common）
├── service         # 业务逻辑接口与实现
├── mapper          # MyBatis Mapper 接口
├── pojo          # 数据库实体类
├── dto             # 请求/响应 DTO
├── utils           # JWT、BCrypt、UserContext 等工具
├── enums           # 状态枚举
├── exception       # 全局异常处理器
├── result          # 统一返回结果封装
└── constant        # 响应码常量

核心功能说明
- 认证：JWT 无状态认证，登录拦截器自动校验，白名单路径免登录

- 密码加密：jBCrypt 哈希存储

- 简历解析：Apache POI 提取 .doc/.docx 文本，HanLP 分词 + TF-IDF 提取关键词

- 匹配度计算：余弦相似度 + 学历/工作年限微调（0~100 分）

- 文件存储：本地存储，通过静态资源映射访问

### 7.API 文档
详细接口定义见 docs/接口文档V1.13.md（包含所有请求/响应格式、错误码说明）

### 8.特别说明
框架中现有的类，以Hello开头的.java文件在后续开发中如果你有实际的代码了就可以删除，
加上这个是因为推送的时候不会上传空包导致框架看起来不是很完整