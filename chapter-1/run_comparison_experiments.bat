@echo off
chcp 65001 >nul
echo ================================================================================
echo 多算法对比实验 - 梯队式完整实验
echo ================================================================================
echo.
echo 实验配置：
echo   - 算法：GA-TS, GA-GA, GA-ACO, ALNS (4个)
echo   - 规模：5个梯队 (小→中小→中→中大→大)
echo   - 算例：每个规模4个案例，共20个
echo   - 重复：每个算例每个算法运行3次
echo   - 时间：每次运行5分钟
echo.
echo 预计时间：启用多线程约5小时，单线程约20小时
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

call mvn exec:java -Dexec.mainClass="MultiAlgorithmComparison" -Dexec.classpathScope=test

echo.
echo ================================================================================
echo 实验完成！
echo ================================================================================
echo.
echo 结果文件已保存在 comparison_experiments_results_* 目录中
echo.

pause
