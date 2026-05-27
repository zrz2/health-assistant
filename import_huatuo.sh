#!/usr/bin/env bash
# ============================================================
# Huatuo26M 知识库一键导入脚本
# 用法: ./import_huatuo.sh
# ============================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
VENV_DIR="$SCRIPT_DIR/venv"
PY_SCRIPT="$SCRIPT_DIR/scripts/import_huatuo.py"

# ---- helpers ----
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()  { echo -e "${CYAN}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()   { echo -e "${RED}[ERR]${NC}   $*"; }

# ---- detect python ----
find_python() {
    for cmd in python3 python; do
        if command -v "$cmd" &>/dev/null; then
            ver=$("$cmd" --version 2>&1 | grep -oP '\d+\.\d+' | head -1)
            major=$(echo "$ver" | cut -d. -f1)
            if [ "$major" -ge 3 ]; then
                echo "$cmd"
                return
            fi
        fi
    done
}

PYTHON=$(find_python)
if [ -z "$PYTHON" ]; then
    err "未找到 Python 3（≥3.9），请先安装 Python"
    echo "  https://www.python.org/downloads/"
    exit 1
fi
ok "Python: $($PYTHON --version)"

# ---- venv ----
if [ ! -d "$VENV_DIR" ]; then
    info "创建虚拟环境..."
    "$PYTHON" -m venv "$VENV_DIR"
    ok "venv 已创建"
fi

case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) ACTIVATE="$VENV_DIR/Scripts/activate" ;;
    *)                    ACTIVATE="$VENV_DIR/bin/activate" ;;
esac

if [ -f "$ACTIVATE" ]; then
    source "$ACTIVATE"
    ok "venv 已激活"
else
    err "未找到虚拟环境激活脚本: $ACTIVATE"
    exit 1
fi

# ---- install deps ----
if [ -f "$SCRIPT_DIR/scripts/requirements.txt" ]; then
    info "安装 Python 依赖..."
    pip install -q -r "$SCRIPT_DIR/scripts/requirements.txt"
    ok "依赖已安装"
fi

# ---- check backend ----
info "检测后端服务..."
BACKEND_OK=0
if command -v curl &>/dev/null; then
    if curl -s --max-time 3 http://localhost:8080/actuator/health > /dev/null 2>&1; then
        BACKEND_OK=1
        ok "后端 :8080 已运行"
    fi
fi
if [ "$BACKEND_OK" -eq 0 ]; then
    warn "后端 :8080 未检测到，请先启动后端：mvn spring-boot:run -DskipTests"
    read -p "是否继续？(y/N) " yes
    [ "$yes" != "y" ] && [ "$yes" != "Y" ] && exit 0
fi

# ---- collect args ----
echo ""
echo -e "${GREEN}============================================================${NC}"
echo -e "${GREEN}   Huatuo26M 知识库导入${NC}"
echo -e "${GREEN}============================================================${NC}"
echo ""

read -p "导入条数 [1000/5000/20000/50000，默认 20000]: " MAX_ITEMS
MAX_ITEMS="${MAX_ITEMS:-20000}"

read -p "子集选择 [kg,encyclopedia,lite 或 all，默认 kg,encyclopedia,lite]: " SUBSETS
if [ -z "$SUBSETS" ] || [ "$SUBSETS" = "all" ]; then
    SUBSETS="kg,encyclopedia,consultation,lite"
fi

read -p "管理员密码 [默认从 .env 读取]: " ADMIN_PASS
if [ -n "$ADMIN_PASS" ]; then
    export ADMIN_PASSWORD="$ADMIN_PASS"
elif [ -f "$SCRIPT_DIR/.env" ]; then
    # try to read admin password from .env (DB_PASSWORD as fallback since admin default password matches)
    source "$SCRIPT_DIR/.env" 2>/dev/null || true
fi

echo ""

# ---- run import ----
"$PYTHON" "$PY_SCRIPT" \
    --max-items "$MAX_ITEMS" \
    --subsets "$SUBSETS" \
    --batch-size 100

echo ""
ok "导入完成"
