@echo off
title HealthAssistant Server
setlocal enabledelayedexpansion

REM ============================================================
REM Health Assistant - Startup Script (Windows)
REM Usage: start.bat   or   start.bat stop
REM ============================================================

set SCRIPT_DIR=%~dp0
set FRONTEND_DIR=%SCRIPT_DIR%frontend
set BACKEND_PORT=8080
set FRONTEND_PORT=5173
set JAR_FILE=%SCRIPT_DIR%target\health-assistant-1.0.0-SNAPSHOT.jar

if "%1"=="stop" goto :STOP

echo.
echo ============================================================
echo     Health Assistant System - Starting...
echo ============================================================
echo.

echo [CHECK] Java...
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found. Install JDK 17+
    pause
    exit /b 1
)
for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do echo [OK]   Java %%i

echo [CHECK] Maven...
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Maven not found. Install Maven 3.9+
    pause
    exit /b 1
)
echo [OK]   Maven

echo [CHECK] Node.js...
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Node.js not found. Install Node.js 18+
    pause
    exit /b 1
)
echo [OK]   Node.js

echo [CHECK] .env ...
if not exist "%SCRIPT_DIR%.env" (
    echo [WARN]  .env not found, copying from template...
    copy "%SCRIPT_DIR%.env.template" "%SCRIPT_DIR%.env" >nul
    echo [WARN]  Please edit .env with your settings, then re-run.
    pause
    exit /b 1
)
echo [OK]   .env

echo.

REM --- Build Backend ---
echo [BUILD] Compiling backend...
cd /d "%SCRIPT_DIR%"
call mvn package -DskipTests -q
if %errorlevel% neq 0 (
    echo [ERROR] Build failed! Running without -q to show errors:
    call mvn package -DskipTests
    pause
    exit /b 1
)
echo [OK]    Build complete

REM --- Start Backend ---
echo [START] Backend (Spring Boot) on port %BACKEND_PORT%...
start "HA-Backend" /B java -jar "%JAR_FILE%"

REM --- Wait for Backend ---
echo [WAIT]  Waiting for backend...
for /l %%i in (1,1,60) do (
    curl -s --max-time 2 http://localhost:%BACKEND_PORT% >nul 2>&1
    if !errorlevel! equ 0 goto :BACKEND_READY
    timeout /t 2 /nobreak >nul
)
echo [ERROR] Backend startup timeout
pause
exit /b 1

:BACKEND_READY
echo [OK]    Backend ready

REM --- Start Frontend ---
echo [START] Frontend (Vue 3 + Vite)...

cd /d "%FRONTEND_DIR%"
if not exist "node_modules\" (
    echo [INSTALL] Installing frontend dependencies...
    call npm install
)

start "HA-Frontend" /B npm run dev -- --host
cd /d "%SCRIPT_DIR%"

REM --- Wait for Frontend ---
echo [WAIT]  Waiting for frontend...
for /l %%i in (1,1,30) do (
    curl -s --max-time 2 http://localhost:%FRONTEND_PORT% >nul 2>&1
    if !errorlevel! equ 0 goto :FRONTEND_READY
    timeout /t 2 /nobreak >nul
)
echo [ERROR] Frontend startup timeout
pause
exit /b 1

:FRONTEND_READY
echo [OK]    Frontend ready

echo.
echo ============================================================
echo     System is running!
echo     Frontend : http://localhost:%FRONTEND_PORT%
echo     Backend  : http://localhost:%BACKEND_PORT%
echo     Swagger  : http://localhost:%BACKEND_PORT%/swagger-ui.html
echo     Stop     : start.bat stop
echo ============================================================
echo.
echo Press any key to run in background (services keep running)...
pause >nul
exit /b 0

:STOP
echo [STOP] Shutting down services...
taskkill /FI "WINDOWTITLE eq HA-Backend" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq HA-Frontend" /F >nul 2>&1
REM Fallback: kill java processes running the health-assistant JAR
for /f "tokens=2" %%p in ('tasklist /FI "IMAGENAME eq java.exe" /FO TABLE /NH ^| findstr /C:"health-assistant"') do (
    taskkill /PID %%p /F >nul 2>&1
) 2>nul
echo [OK]   Services stopped
exit /b 0
