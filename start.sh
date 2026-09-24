#!/bin/bash
# ==============================================
# 园区公共饮水机管理系统 - 启动脚本
# 自动构建、启动服务、端口自检、输出访问地址
# ==============================================

set -e

# 加载 .env 环境变量
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
else
    echo "❌ 错误：.env 配置文件不存在"
    exit 1
fi

echo "=============================================="
echo "  园区公共饮水机管理系统 - 启动"
echo "=============================================="

# ---------- 步骤 1：停止现有服务 ----------
echo ""
echo "[1/5] 停止现有服务..."
docker-compose -p ${COMPOSE_PROJECT_NAME} down 2>/dev/null || true

# ---------- 步骤 2：端口占用检查 ----------
echo ""
echo "[2/5] 端口占用检查..."

PORTS=("$FRONTEND_PORT" "$BACKEND_PORT" "$MYSQL_PORT" "$REDIS_PORT")
PORT_NAMES=("前端" "后端" "MySQL" "Redis")

for i in "${!PORTS[@]}"; do
    PORT=${PORTS[$i]}
    NAME=${PORT_NAMES[$i]}
    if lsof -nP -iTCP:${PORT} -sTCP:LISTEN > /dev/null 2>&1; then
        PID=$(lsof -nP -iTCP:${PORT} -sTCP:LISTEN -t 2>/dev/null | head -1)
        if [ -n "$PID" ]; then
            PROCESS=$(ps -p $PID -o comm= 2>/dev/null || echo "未知进程")
            echo "❌ 端口 ${PORT} (${NAME}) 被占用，进程ID: ${PID} (${PROCESS})"
            echo "   请先停止该进程或修改 .env 中的端口配置"
            exit 1
        fi
    else
        echo "✅ 端口 ${PORT} (${NAME}) 可用"
    fi
done

# ---------- 步骤 3：构建并启动服务 ----------
echo ""
echo "[3/5] 构建并启动 Docker 服务（首次构建较慢，请耐心等待）..."
docker-compose -p ${COMPOSE_PROJECT_NAME} up -d --build

# ---------- 步骤 4：等待服务健康检查通过 ----------
echo ""
echo "[4/5] 等待服务健康检查通过..."

check_health() {
    local container=$1
    local service=$2
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        HEALTH=$(docker inspect --format='{{.State.Health.Status}}' ${container} 2>/dev/null || echo "starting")

        if [ "$HEALTH" = "healthy" ]; then
            echo "✅ ${service} 服务就绪"
            return 0
        elif [ "$HEALTH" = "unhealthy" ]; then
            echo "❌ ${service} 服务启动失败"
            docker logs ${container} --tail 30
            return 1
        fi

        echo "  等待 ${service} 中... (${attempt}/${max_attempts})"
        sleep 3
        attempt=$((attempt + 1))
    done

    echo "❌ ${service} 服务启动超时"
    return 1
}

check_health "${COMPOSE_PROJECT_NAME}-mysql" "MySQL"
check_health "${COMPOSE_PROJECT_NAME}-redis" "Redis"

# 后端没有健康检查，单独检查
echo "  等待后端启动中..."
sleep 20

# ---------- 步骤 5：访问自检 ----------
echo ""
echo "[5/5] 访问地址自检..."

FRONTEND_URL_127="http://127.0.0.1:${FRONTEND_PORT}"
FRONTEND_URL_LOCAL="http://localhost:${FRONTEND_PORT}"
BACKEND_URL="http://127.0.0.1:${BACKEND_PORT}/api/building-group/tree"

# 检查前端页面标题一致性
check_frontend() {
    local url=$1
    local label=$2

    for i in {1..10}; do
        TITLE=$(curl -sS "$url" 2>/dev/null | grep -o '<title>[^<]*</title>' | sed 's/<[^>]*>//g' || true)
        if [ -n "$TITLE" ]; then
            echo "✅ ${label} 页面标题: ${TITLE}"
            return 0
        fi
        sleep 2
    done
    echo "❌ ${label} 访问失败"
    return 1
}

check_frontend "$FRONTEND_URL_127" "127.0.0.1"
check_frontend "$FRONTEND_URL_LOCAL" "localhost"

# 检查API是否正常
echo "  检查后端API..."
API_RESPONSE=$(curl -sS "$BACKEND_URL" 2>/dev/null | head -c 100 || true)
if [ -n "$API_RESPONSE" ]; then
    echo "✅ 后端API响应正常"
else
    echo "⚠️  后端API未响应（可能还在启动中）"
fi

# ---------- 输出访问地址 ----------
echo ""
echo "=============================================="
echo "  ✅ 系统启动成功！"
echo "=============================================="
echo ""
echo "  🌐 前端访问地址（推荐）:"
echo "     ${FRONTEND_URL_LOCAL}"
echo ""
echo "  🔗 直连IP:"
echo "     ${FRONTEND_URL_127}"
echo ""
echo "  🔧 后端API:"
echo "     http://127.0.0.1:${BACKEND_PORT}/api"
echo ""
echo "  💾 数据库端口: ${MYSQL_PORT}"
echo "  📦 Redis端口: ${REDIS_PORT}"
echo ""
echo "  📋 查看日志: docker-compose -p ${COMPOSE_PROJECT_NAME} logs -f"
echo "  ⏹️  停止服务: docker-compose -p ${COMPOSE_PROJECT_NAME} down"
echo ""
echo "=============================================="
