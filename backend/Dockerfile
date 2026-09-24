# ==============================================
# 后端 Dockerfile - 园区公共饮水机管理系统
# 构建阶段：依赖层 + 源码层 分离，实现Docker原生缓存
# 基础镜像统一使用 DOCKER_REGISTRY 环境变量前缀
# ==============================================

ARG DOCKER_REGISTRY=docker.m.daocloud.io/library/

# ---------- 构建阶段 ----------
FROM ${DOCKER_REGISTRY}maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 设置国内 Maven 镜像源
COPY settings.xml /root/.m2/settings.xml

# 仅复制 pom.xml，利用 Docker 缓存，仅当 pom.xml 变更时重新下载依赖
COPY pom.xml ./

# 预下载全部依赖，只要 pom.xml 不变更，这一步将复用缓存
RUN mvn compile -Dmaven.test.skip=true -Dspring-boot.repackage.skip=true -B

# 最后才复制源码，仅当源码变更时重新编译
COPY src ./src

# 编译打包，跳过测试
RUN mvn clean package -Dmaven.test.skip=true -B

# ---------- 运行阶段 ----------
FROM ${DOCKER_REGISTRY}eclipse-temurin:17-jre

WORKDIR /app

# 配置时区
RUN apt-get update && \
    apt-get install -y --no-install-recommends tzdata && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    rm -rf /var/lib/apt/lists/*

# 从构建阶段复制编译好的 jar 包
COPY --from=builder /app/target/water-dispenser.jar app.jar

# 环境变量（可被 docker-compose 覆盖）
ENV MYSQL_HOST=mysql
ENV MYSQL_PORT=3306
ENV MYSQL_DATABASE=xgs_water
ENV MYSQL_USER=root
ENV MYSQL_PASSWORD=xgs2024
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379
ENV REDIS_PASSWORD=
ENV REDIS_DATABASE=0
ENV TZ=Asia/Shanghai

# 容器内部服务端口（固定为 8088，不映射到宿主机）
EXPOSE 8088

# 启动命令
ENTRYPOINT ["java", "-jar", "-Xms256m", "-Xmx1024m", "-Duser.timezone=Asia/Shanghai", "app.jar"]
