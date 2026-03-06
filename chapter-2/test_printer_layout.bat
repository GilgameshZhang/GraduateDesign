@echo off
echo ========================================
echo    测试打印机布局可视化
echo ========================================
echo.

cd /d %~dp0

echo [1/3] 编译项目...
call mvn compile -q 2>nul
if %errorlevel% neq 0 (
    echo 编译失败！
    pause
    exit /b 1
)
echo ✓ 编译成功

echo.
echo [2/3] 运行打印布局测试...
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
    dir /b test_visualization_results\*.png 2>nul
    if %errorlevel% neq 0 (
        echo ⚠️  未找到PNG文件
    )
) else (
    echo ⚠️ 可视化结果目录不存在
)

echo.
echo ========================================
echo          测试完成！
echo ========================================
echo.
echo 如果看到打印批次的布局图生成了，说明功能正常。
echo 每个批次会生成一个单独的PNG文件，显示该批次中
echo 所有零件的矩形布局。
echo.
pause
