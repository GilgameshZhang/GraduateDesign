@echo off
chcp 65001 >nul
echo ========================================
echo 快速可视化测试
echo ========================================
echo.
echo 这个测试会：
echo - 验证可视化功能是否正常
echo - 生成示例迭代曲线图
echo - 不运行完整算法（节省时间）
echo.
echo 预计耗时：30秒
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
echo 开始快速测试...
call mvn exec:java -Dexec.mainClass="VisualizationTest" -Dexec.classpathScope=test

echo.
echo ========================================
echo 测试完成！
echo 结果保存在：chapter-1/src/main/output/test/
echo ========================================
pause
