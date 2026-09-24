# ==============================================
# 前端 Dockerfile - 园区公共饮水机管理系统
# 构建阶段：依赖层 + 源码层 分离，实现Docker原生缓存
# 使用中科大 NPM 镜像源，无需 VPN
# ==============================================

ARG DOCKER_REGISTRY=docker.m.daocloud.io/library/

# ---------- 构建阶段 ----------
FROM ${DOCKER_REGISTRY}node:20-alpine AS builder

WORKDIR /app

# 配置 NPM 国内镜像源（中科大）
ENV NPM_CONFIG_REGISTRY=https://npmreg.proxy.ustclug.org/
ENV NPM_CONFIG_DISTURL=https://mirrors.ustc.edu.cn/node/

# 仅复制 package.json 和 package-lock.json（如果存在）
# 只要这两个文件不变，npm ci 将复用 Docker 缓存
COPY package.json ./
COPY package-lock.json* ./

# 安装依赖，使用 npm ci 确保版本完全匹配
# 仅当 package.json/package-lock.json 变更时才会重新执行
RUN npm ci --no-audit --no-fund --loglevel=error

# 最后才复制全部源码，仅当源码变更时重新构建
COPY . .

# 生产构建
RUN npm run build

# ---------- 运行阶段 ----------
FROM ${DOCKER_REGISTRY}nginx:alpine

WORKDIR /usr/share/nginx/html

# 配置时区
RUN apk add --no-cache tzdata && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 容器内部端口（固定 80）
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://127.0.0.1/ || exit 1

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]
