#!/usr/bin/env bash
# ============================================================
# 医疗健康智能客服系统 — 一键启动脚本
# 用法: ./start.sh [dev|build|stop]
#   dev   - 开发模式：构建 JAR + 启动（默认）
#   build - 同 dev（保留兼容）
#   stop  - 停止所有后台服务
# ============================================================
set -e

# ---------- 配置 ----------
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_DIR="$SCRIPT_DIR/frontend"
BACKEND_PORT=8080
FRONTEND_PORT=5173
JAR_FILE="$SCRIPT_DIR/target/health-assistant-1.0.0-SNAPSHOT.jar"
PID_DIR="$SCRIPT_DIR/.pids"
BACKEND_PID_FILE="$PID_DIR/backend.pid"
FRONTEND_PID_FILE="$PID_DIR/frontend.pid"

# ---------- 颜色 ----------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

info()  { echo -e "${CYAN}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()   { echo -e "${RED}[ERR]${NC}   $*"; }
step()  { echo -e "${BLUE}[STEP]${NC}  $*"; }

# ---------- 清理 ----------
cleanup() {
    echo ""
    info "正在关闭服务..."
    [ -f "$BACKEND_PID_FILE" ] && kill "$(cat "$BACKEND_PID_FILE")" 2>/dev/null && rm -f "$BACKEND_PID_FILE"
    [ -f "$FRONTEND_PID_FILE" ] && kill "$(cat "$FRONTEND_PID_FILE")" 2>/dev/null && rm -f "$FRONTEND_PID_FILE"
    ok "所有服务已关闭"
}
trap cleanup EXIT INT TERM

# ---------- 检查依赖 ----------
check_prerequisites() {
    step "检查运行环境..."

    # Java 17+
    if ! command -v java &>/dev/null; then
        err "未找到 Java，请安装 JDK 17+"
        exit 1
    fi
    JAVA_VER=$(java -version 2>&1 | head -1 | grep -oP '\d+' | head -1)
    if [ "$JAVA_VER" -lt 17 ] 2>/dev/null; then
        warn "Java 版本为 $JAVA_VER，建议使用 JDK 17+"
    else
        ok "Java $JAVA_VER"
    fi

    # Maven
    if ! command -v mvn &>/dev/null; then
        err "未找到 Maven，请安装 Maven 3.9+"
        exit 1
    fi
    ok "Maven $(mvn --version 2>/dev/null | head -1 | awk '{print $3}')"

    # Node.js
    if ! command -v node &>/dev/null; then
        err "未找到 Node.js，请安装 Node.js 18+"
        exit 1
    fi
    ok "Node $(node --version)"

    # .env
    if [ ! -f "$SCRIPT_DIR/.env" ]; then
        warn ".env 文件不存在，正在从模板创建..."
        cp "$SCRIPT_DIR/.env.template" "$SCRIPT_DIR/.env"
        warn "请编辑 .env 填入配置后重新运行"
        exit 1
    fi
    ok ".env 已就绪"
}

# ---------- 检查外部服务 ----------
check_services() {
    step "检查外部服务..."

    # MySQL
    if curl -s --max-time 2 telnet://localhost:3306 &>/dev/null; then
        ok "MySQL :3306 已连接"
    else
        warn "MySQL :3306 无法连接，请确认已启动"
    fi

    # Redis
    if curl -s --max-time 2 telnet://localhost:6379 &>/dev/null; then
        ok "Redis :6379 已连接"
    else
        warn "Redis :6379 无法连接，请确认已启动"
    fi

    # Elasticsearch
    if curl -s --max-time 3 http://localhost:9200 &>/dev/null; then
        ok "Elasticsearch :9200 已连接"
    else
        warn "Elasticsearch :9200 无法连接（RAG 功能需要）"
    fi
}

# ---------- 等待端口 ----------
wait_for_port() {
    local host=$1 port=$2 desc=$3 timeout=${4:-120}
    step "等待 $desc ($host:$port)..."
    local elapsed=0
    while [ $elapsed -lt $timeout ]; do
        if curl -s --max-time 2 "http://$host:$port" &>/dev/null; then
            ok "$desc 已就绪 ($host:$port)"
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
        echo -n "."
    done
    echo ""
    err "$desc 启动超时 ($timeout s)"
    return 1
}

# ---------- 启动后端 ----------
start_backend() {
    step "构建并启动后端 (Spring Boot)..."

    cd "$SCRIPT_DIR"

    # 加载 .env
    set -a; source "$SCRIPT_DIR/.env"; set +a

    info "编译后端..."
    mvn package -DskipTests -q
    info "启动 JAR..."
    java -jar "$JAR_FILE" &
    echo $! > "$BACKEND_PID_FILE"
}

# ---------- 启动前端 ----------
start_frontend() {
    step "启动前端 (Vue 3 + Vite)..."
    cd "$FRONTEND_DIR"

    if [ ! -d "node_modules" ]; then
        info "安装前端依赖..."
        npm install --silent
    fi

    npm run dev -- --host &
    echo $! > "$FRONTEND_PID_FILE"
    cd "$SCRIPT_DIR"
}

# ---------- 停止服务 ----------
stop_services() {
    step "停止服务..."
    if [ -f "$BACKEND_PID_FILE" ]; then
        PID=$(cat "$BACKEND_PID_FILE")
        kill "$PID" 2>/dev/null && ok "已停止后端 (PID $PID)" || warn "后端进程不存在"
        rm -f "$BACKEND_PID_FILE"
    fi
    if [ -f "$FRONTEND_PID_FILE" ]; then
        PID=$(cat "$FRONTEND_PID_FILE")
        kill "$PID" 2>/dev/null && ok "已停止前端 (PID $PID)" || warn "前端进程不存在"
        rm -f "$FRONTEND_PID_FILE"
    fi
}

# ---------- 主流程 ----------
case "${1:-dev}" in
    stop)
        stop_services
        exit 0
        ;;
    dev|build)
        echo ""
        echo -e "${GREEN}============================================================${NC}"
        echo -e "${GREEN}   医疗健康智能客服系统 — 启动中...${NC}"
        echo -e "${GREEN}============================================================${NC}"
        echo ""

        check_prerequisites
        echo ""
        check_services
        echo ""

        mkdir -p "$PID_DIR"

        # 移除 EXIT trap，避免 stop 命令之后 cleanup 杀进程
        trap - EXIT INT TERM

        start_backend

        # 等待后端启动
        wait_for_port localhost "$BACKEND_PORT" "后端"

        start_frontend

        # 等待前端启动
        wait_for_port localhost "$FRONTEND_PORT" "前端" 60

        echo ""
        echo -e "${GREEN}============================================================${NC}"
        echo -e "${GREEN}   系统已启动！${NC}"
        echo -e "${GREEN}   前端:  http://localhost:${FRONTEND_PORT}${NC}"
        echo -e "${GREEN}   后端:  http://localhost:${BACKEND_PORT}${NC}"
        echo -e "${GREEN}   API文档: http://localhost:${BACKEND_PORT}/swagger-ui.html${NC}"
        echo -e "${GREEN}   停止:  ./start.sh stop${NC}"
        echo -e "${GREEN}============================================================${NC}"

        # 注册 cleanup 以便 Ctrl+C 时清理
        trap cleanup EXIT INT TERM

        info "按 Ctrl+C 停止所有服务"
        wait
        ;;
    *)
        echo "用法: $0 [dev|build|stop]"
        echo "  dev   - 开发模式启动（默认）"
        echo "  build - 同 dev（保留兼容）"
        echo "  stop  - 停止所有后台服务"
        exit 1
        ;;
esac
