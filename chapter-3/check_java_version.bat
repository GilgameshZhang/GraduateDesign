@echo off
chcp 65001 >nul
echo ════════════════════════════════════════
echo   Java 环境检查
echo ════════════════════════════════════════
echo.

echo 检查 Java 版本...
echo.

java -version 2>&1 | findstr /i "version"

if %errorlevel% neq 0 (
    echo ❌ 未检测到 Java！
    echo.
    echo 请先安装 JDK 8 或更高版本
    echo 下载地址: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

echo.
echo ────────────────────────────────────────
echo.

echo 检查 Maven...
echo.

call mvn -version 2>&1 | findstr /i "Maven"

if %errorlevel% neq 0 (
    echo ❌ 未检测到 Maven！
    echo.
    echo 请先安装 Maven
    echo 下载地址: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

echo.
echo ════════════════════════════════════════
echo   环境检查通过！
echo ════════════════════════════════════════
echo.
echo ✅ Java 已安装
echo ✅ Maven 已安装
echo.
echo 项目要求：JDK 8 或更高版本
echo.
echo 如果编译出错，请确保：
echo   1. 使用 JDK（不是 JRE）
echo   2. JAVA_HOME 环境变量已设置
echo   3. Java 版本 >= 1.8
echo.

pause
