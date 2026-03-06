@echo off
chcp 65001 >nul
echo ═══════════════════════════════════════════════════════════════
echo     NSGA-II 田口实验 - 快速测试
echo ═══════════════════════════════════════════════════════════════
echo.
echo 注意：这是快速测试版本，只运行3次而不是10次
echo.

cd /d "%~dp0"

echo 正在编译项目...
echo.

call mvn clean compile test-compile -q

if %errorlevel% neq 0 (
    echo.
    echo ❌ 编译失败！请检查代码错误
    pause
    exit /b 1
)

echo ✅ 编译成功
echo.

REM 修改重复次数为3次进行快速测试
echo 提示：快速测试会运行3次而不是10次
echo       如需完整实验，请使用 run_taguchi_experiment.bat
echo.
pause

call mvn test -Dtest=NSGAIITaguchiExperimentRunner -q

echo.
echo 测试完成！
pause
