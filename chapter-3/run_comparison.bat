@echo off
chcp 65001 >nul
echo ════════════════════════════════════════════════════════════════
echo   算法对比实验 - 快速启动脚本
echo ════════════════════════════════════════════════════════════════
echo.

echo 正在编译项目...
call mvn clean compile test-compile

if %errorlevel% neq 0 (
    echo.
    echo ❌ 编译失败！请检查代码错误。
    pause
    exit /b 1
)

echo.
echo ✅ 编译成功！
echo.
echo 正在启动对比实验...
echo.

call mvn test -Dtest=AlgorithmComparisonExperimentRunner

echo.
echo ════════════════════════════════════════════════════════════════
echo   实验完成
echo ════════════════════════════════════════════════════════════════
echo.
echo 结果保存在: output\comparison_experiment\
echo.
pause
