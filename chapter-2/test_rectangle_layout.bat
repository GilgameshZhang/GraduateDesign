@echo off
echo ========================================
echo    测试矩形零件布局可视化
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
    echo 生成的布局图文件:
    dir /b test_visualization_results\printer_layout_*.png 2>nul
    if %errorlevel% neq 0 (
        echo ⚠️  未找到打印布局PNG文件
    ) else (
        echo.
        echo 查看结果: test_visualization_results\printer_layout_printer1_batch1.png
        echo 查看结果: test_visualization_results\printer_layout_printer1_batch2.png
    )
) else (
    echo ⚠️ 可视化结果目录不存在
)

echo.
echo ========================================
echo          测试完成！
echo ========================================
echo.
echo 新功能特点:
echo • 每个批次生成单独的布局图
echo • 零件严格按l(长)和w(宽)绘制矩形
echo • 旋转零件显示红色旋转标记↻
echo • xy坐标表示矩形左下角位置
echo.
pause
