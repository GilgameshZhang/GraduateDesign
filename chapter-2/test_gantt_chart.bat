@echo off
echo ========================================
echo    测试新的甘特图可视化
echo ========================================
echo.

cd /d %~dp0

echo [1/3] 清理旧的测试结果...
if exist "test_visualization_results" rmdir /s /q test_visualization_results

echo.
echo [2/3] 编译并运行测试...
java -cp "target/classes;target/dependency/*" SimpleVisualizationTest

if %errorlevel% neq 0 (
    echo 测试运行失败！
    pause
    exit /b 1
)

echo.
echo [3/3] 检查生成的文件...
if exist "test_visualization_results" (
    echo ✓ 可视化结果目录存在
    echo 生成的图表文件:
    dir /b test_visualization_results\*.png 2>nul
    if %errorlevel% neq 0 (
        echo ⚠️  未找到PNG文件
    ) else (
        echo.
        echo 查看结果: test_visualization_results\gantt_chart.png
        echo 查看结果: test_visualization_results\printer_layout_printer1_batch*.png
    )
) else (
    echo ⚠️ 可视化结果目录不存在
)

echo.
echo ========================================
echo          测试完成！
echo ========================================
echo.
echo 新甘特图特点:
echo • 横轴：时间轴
echo • 纵轴：机器编号
echo • 矩形标签：
echo   - 打印机/离散机：J工件号
echo   - 批处理机：B机器号批次号
echo • 彩色矩形表示不同操作
echo.
pause
