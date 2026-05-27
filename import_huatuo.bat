@echo off
:: ============================================================
:: Huatuo26M 知识库一键导入脚本 (Windows)
:: 双击运行，或在命令提示符中执行: import_huatuo.bat
:: ============================================================
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "VENV_DIR=%SCRIPT_DIR%venv"
set "PY_SCRIPT=%SCRIPT_DIR%scripts\import_huatuo.py"
set "REQ_FILE=%SCRIPT_DIR%scripts\requirements.txt"

echo.
echo ============================================================
echo    Huatuo26M 知识库导入
echo ============================================================
echo.

:: ---- detect python ----
set "PYTHON="
where python >nul 2>&1 && set "PYTHON=python"
where python3 >nul 2>&1 && set "PYTHON=python3"
where py >nul 2>&1 && set "PYTHON=py"

if "%PYTHON%"=="" (
    where "%LOCALAPPDATA%\Programs\Python\Python3*\python.exe" >nul 2>&1 && (
        for /f "delims=" %%p in ('where "%LOCALAPPDATA%\Programs\Python\Python3*\python.exe" 2^>nul') do set "PYTHON=%%p"
    )
)

if "%PYTHON%"=="" (
    echo [ERR] 未找到 Python 3 ^(>=3.9^)，请先安装 Python
    echo  https://www.python.org/downloads/
    pause
    exit /b 1
)
echo [OK]   Python: %PYTHON%

:: ---- venv ----
if not exist "%VENV_DIR%\Scripts\python.exe" (
    echo [INFO] 创建虚拟环境...
    "%PYTHON%" -m venv "%VENV_DIR%"
    if errorlevel 1 (
        echo [ERR] 创建虚拟环境失败
        pause
        exit /b 1
    )
    echo [OK]   venv 已创建
)

call "%VENV_DIR%\Scripts\activate.bat"
if errorlevel 1 (
    echo [ERR] 激活虚拟环境失败
    pause
    exit /b 1
)
echo [OK]   venv 已激活

:: ---- install deps ----
if exist "%REQ_FILE%" (
    echo [INFO] 安装 Python 依赖...
    pip install -q -r "%REQ_FILE%"
    echo [OK]   依赖已安装
)

:: ---- check backend ----
echo [INFO] 检测后端服务...
set BACKEND_OK=0
curl -s --max-time 3 http://localhost:8080/actuator/health >nul 2>&1 && set BACKEND_OK=1
if !BACKEND_OK!==1 (
    echo [OK]   后端 :8080 已运行
) else (
    echo [WARN] 后端 :8080 未检测到，请先启动后端：mvn spring-boot:run -DskipTests
    set /p YES="是否继续？(y/N) "
    if /i not "!YES!"=="y" exit /b 0
)

:: ---- collect args ----
echo.
set /p MAX_ITEMS="导入条数 [1000/5000/20000/50000，默认 20000]: "
if "%MAX_ITEMS%"=="" set MAX_ITEMS=20000

echo 子集: kg=知识图谱, encyclopedia=百科, consultation=问诊, lite=精简版
set /p SUBSETS="选择子集 [逗号分隔，默认 kg,encyclopedia,lite，all=全部]: "
if "%SUBSETS%"=="" set SUBSETS=kg,encyclopedia,lite
if /i "%SUBSETS%"=="all" set SUBSETS=kg,encyclopedia,consultation,lite

set /p ADMIN_PASS="管理员密码 [默认从 .env 读取，直接回车跳过]: "
if not "%ADMIN_PASS%"=="" set ADMIN_PASSWORD=%ADMIN_PASS%

echo.

:: ---- run import ----
"%PYTHON%" "%PY_SCRIPT%" --max-items %MAX_ITEMS% --subsets %SUBSETS% --batch-size 100

echo.
echo [OK] 导入完成
pause
