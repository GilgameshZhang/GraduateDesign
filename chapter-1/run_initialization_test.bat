@echo off
chcp 65001 >nul
echo ================================================================================
echo 初始化策略消融实验测试
echo ================================================================================
echo.
echo 实验内容：
echo   - 测试5种初始化比例：0%%, 25%%, 50%%, 75%%, 100%%启发式
echo   - 每种配置运行3次
echo   - 每次运行1分钟（快速测试）
echo.
echo 预计时间：约5-8分钟
echo.
echo ================================================================================
echo.

pause

echo.
echo 正在编译项目...
call mvn clean compile test-compile -DskipTests
if %ERRORLEVEL% neq 0 (
    echo.
    echo 编译失败！请检查错误信息。
    pause
    exit /b 1
)

echo.
echo ================================================================================
echo 开始运行实验...
echo ================================================================================
echo.

call mvn exec:java -Dexec.mainClass="InitializationStrategyTest" -Dexec.classpathScope=test

echo.
echo ================================================================================
echo 实验完成！
echo ================================================================================
echo.
echo 结果文件：initialization_strategy_test_results.csv
echo.

pause
