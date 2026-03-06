@echo off
echo ========================================
echo    测试批处理机标签显示
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
        echo 批处理机标签应该显示为 B+机器号+批次号 格式
        echo 例如: B11 (1号批处理机的第1个批次)
        echo       B22 (2号批处理机的第2个批次)
    )
) else (
    echo ⚠️ 可视化结果目录不存在
)

echo.
echo ========================================
echo          测试完成！
echo ========================================
echo.
echo 批处理机标签格式:
echo • B11 = 批处理机1的批次1
echo • B12 = 批处理机1的批次2
echo • B21 = 批处理机2的批次1
echo • B22 = 批处理机2的批次2
echo.
pause
