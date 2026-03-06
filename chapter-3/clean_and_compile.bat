@echo off
chcp 65001 >nul
echo ════════════════════════════════════════
echo   清理并重新编译项目
echo ════════════════════════════════════════
echo.

cd /d "%~dp0"

echo [1/3] 清理旧的编译文件...
call mvn clean -q
if %errorlevel% neq 0 (
    echo ❌ 清理失败
    pause
    exit /b 1
)
echo ✅ 清理完成
echo.

echo [2/3] 重新编译项目...
call mvn compile -q
if %errorlevel% neq 0 (
    echo ❌ 编译失败！
    echo.
    echo 可能的原因：
    echo   1. Java 版本不匹配（需要 JDK 8+）
    echo   2. 依赖下载失败
    echo   3. 代码语法错误
    echo.
    echo 请检查详细错误信息：
    call mvn compile
    pause
    exit /b 1
)
echo ✅ 编译成功
echo.

echo [3/3] 编译测试代码...
call mvn test-compile -q
if %errorlevel% neq 0 (
    echo ❌ 测试代码编译失败！
    call mvn test-compile
    pause
    exit /b 1
)
echo ✅ 测试代码编译成功
echo.

echo ════════════════════════════════════════
echo   编译完成！
echo ════════════════════════════════════════
echo.
echo 现在可以运行实验了：
echo   run_taguchi_experiment.bat
echo.

pause
