@echo off
echo === 编译和测试可视化功能 ===

cd /d %~dp0

echo 1. 下载Maven依赖...
call mvn dependency:copy-dependencies -q

echo 2. 编译Java代码...
call mvn compile -q

if %errorlevel% neq 0 (
    echo 编译失败！
    pause
    exit /b 1
)

echo 3. 运行可视化测试...
java -cp "target/classes;target/dependency/*" test.TestVisualization

echo.
echo 测试完成！
pause
