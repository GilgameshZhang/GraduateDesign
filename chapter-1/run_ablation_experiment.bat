@echo off
chcp 65001 >nul
echo ========================================
echo GA-TS消融实验启动器
echo ========================================
echo.
echo 实验配置：
echo - 配置：5个（V0-Full, V1-NoHeuristic, V2-NoTabu, V3-SimpleHeuristic, V4-Minimal）
echo - 算例：3个（Small, Medium, Large）
echo - 每个配置运行10次
echo - 每次运行5分钟
echo - 多线程并行执行
echo - 自动生成可视化图表
echo.
echo 预计耗时（8核）：约47分钟
echo ========================================
echo.

cd /d "%~dp0"

echo 开始编译...
call mvn clean compile test-compile -DskipTests
if %errorlevel% neq 0 (
    echo 编译失败！
    pause
    exit /b 1
)

echo.
echo 开始运行消融实验...
echo 注意：这可能需要较长时间，请耐心等待
call mvn exec:java -Dexec.mainClass="AblationExperimentRunner" -Dexec.classpathScope=test

echo.
echo ========================================
echo 消融实验完成！
echo 结果保存在：chapter-1/src/main/output/ablation/
echo ========================================
pause
