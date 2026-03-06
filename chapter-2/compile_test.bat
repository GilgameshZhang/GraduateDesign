@echo off
echo === 编译测试 SimpleVisualizationTest.java ===

cd /d %~dp0

echo 1. 编译主类...
javac -cp "target/classes" -d target/test-classes src/test/java/SimpleVisualizationTest.java

if %errorlevel% neq 0 (
    echo 编译失败！
    pause
    exit /b 1
)

echo 2. 运行测试...
java -cp "target/classes;target/test-classes" SimpleVisualizationTest

echo.
echo 测试完成！
pause
