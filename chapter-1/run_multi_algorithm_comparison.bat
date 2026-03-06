@echo off
chcp 65001 >nul
echo ========================================
echo 多算法对比实验启动器
echo ========================================
echo.
echo 实验配置：
echo - 算法：GA-TS, GA-GA, GA-ACO, ALNS
echo - 每个算法运行3次
echo - 每次运行5分钟
echo - 多线程并行执行
echo - 自动生成可视化图表
echo.
echo 预计耗时（8核）：约7.5分钟
echo ========================================
echo.

cd /d "%~dp0"

echo 开始编译...
call mvn clean compile -DskipTests
if %errorlevel% neq 0 (
    echo 编译失败！
    pause
    exit /b 1
)

echo.
echo 开始运行实验...
call mvn exec:java -Dexec.mainClass="MultiAlgorithmComparison" -Dexec.classpathScope=test

echo.
echo ========================================
echo 实验完成！
echo 结果保存在：chapter-1/src/main/output/multi_algorithm_comparison/
echo ========================================
pause
