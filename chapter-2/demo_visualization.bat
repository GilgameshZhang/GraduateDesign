@echo off
echo ========================================
echo    遗传算法调度可视化演示
echo ========================================
echo.

cd /d %~dp0

echo [1/4] 检查算例文件...
if not exist "src\main\resources\instance\J20P3B2D5_01.txt" (
    echo 错误：找不到算例文件！
    echo 请确保算例文件存在。
    pause
    exit /b 1
)
echo ✓ 算例文件存在

echo.
echo [2/4] 下载依赖库...
call mvn dependency:copy-dependencies -q 2>nul
if %errorlevel% neq 0 (
    echo 警告：Maven依赖下载失败，可能需要网络连接。
    echo 将尝试使用已下载的依赖继续...
)

echo.
echo [3/4] 编译项目...
call mvn compile -q 2>nul
if %errorlevel% neq 0 (
    echo 错误：编译失败！
    echo 请检查代码是否有语法错误。
    pause
    exit /b 1
)
echo ✓ 编译成功

echo.
echo [4/4] 运行可视化演示...
echo 注意：这是一个简化测试，不运行完整的GA算法
echo.

java -cp "target/classes;target/dependency/*" SimpleVisualizationTest

if %errorlevel% neq 0 (
    echo.
    echo 演示运行失败！
    echo 可能是依赖库问题，请检查Maven配置。
    pause
    exit /b 1
)

echo.
echo ========================================
echo          演示完成！
echo ========================================
echo.
echo 生成的文件位于: visualization_results\
echo.
echo 文件列表:
echo   - gantt_chart.png        (甘特图)
echo   - printer_layout_*.png   (打印机布局图)
echo   - summary_report.json    (汇总报告)
echo.
echo 提示: 运行完整GA算法会生成更详细的结果
echo 命令: java -cp "target/classes;target/dependency/*" test.RunAlgorithmExample
echo.

pause
