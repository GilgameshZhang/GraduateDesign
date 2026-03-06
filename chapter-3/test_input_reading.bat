@echo off
chcp 65001 > nul
echo ============================================================
echo 测试Input类读取功能
echo ============================================================
echo.

cd chapter-3

echo [1/2] 编译测试程序...
if not exist "bin" mkdir bin

javac -encoding UTF-8 -d bin -sourcepath src/main/java ^
    src/main/java/util/java/TestInputReading.java ^
    src/main/java/ProgramEntity/*.java ^
    src/main/java/ProgramEntity/Machine/*.java ^
    src/main/java/ProblemFrame/*.java

if errorlevel 1 (
    echo.
    echo ❌ 编译失败！
    pause
    exit /b 1
)
echo ✓ 编译成功
echo.

echo [2/2] 运行测试...
java -cp bin util.java.TestInputReading

if errorlevel 1 (
    echo.
    echo ❌ 测试失败！
    pause
    exit /b 1
)

echo.
echo ============================================================
echo 测试完成
echo ============================================================
pause
