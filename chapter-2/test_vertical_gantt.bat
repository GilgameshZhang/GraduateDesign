@echo off
echo ========================================
echo    测试纵轴时间甘特图
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
echo • 横轴：机器编号 (打印机1、批处理机1等)
echo • 纵轴：时间轴 (从上到下时间递增)
echo • 矩形：宽度固定，高度表示持续时间
echo • 标签：机器名标注在X轴下方
echo • 工序：打印机和批处理机显示B机器号批次号，离散机显示J工件号
echo • 监控：每一代打印平均makespan和最小makespan
echo.
pause
