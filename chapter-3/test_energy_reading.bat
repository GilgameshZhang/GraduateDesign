@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo 编译并测试能耗参数读取
echo ========================================
echo.

cd /d "%~dp0"

set SRC=chapter-3\src\main\java
set BIN=chapter-3\target\test-classes

echo [1/3] 清理旧的class文件...
if exist "%BIN%\util\java\TestEnergyReading.class" del "%BIN%\util\java\TestEnergyReading.class"

echo [2/3] 编译测试程序...
javac -encoding UTF-8 -d "%BIN%" -cp "%BIN%" ^
    "%SRC%\util\java\TestEnergyReading.java" ^
    "%SRC%\ProgramEntity\EnergyAwareInput.java" ^
    "%SRC%\ProgramEntity\Input.java" ^
    "%SRC%\ProgramEntity\Problem.java" ^
    "%SRC%\ProgramEntity\Item.java" ^
    "%SRC%\ProgramEntity\Job.java" ^
    "%SRC%\ProgramEntity\Machine\Machine.java" ^
    "%SRC%\ProgramEntity\Machine\PrintMachine.java" ^
    "%SRC%\ProgramEntity\Machine\BathchMachine.java" ^
    "%SRC%\ProgramEntity\Machine\DiscreteProcessingMachine.java" ^
    "%SRC%\ProblemFrame\PowerParameters.java"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ 编译失败！
    pause
    exit /b 1
)

echo ✅ 编译成功！
echo.
echo [3/3] 运行测试...
echo ----------------------------------------
java -cp "%BIN%" util.java.TestEnergyReading

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ 运行失败！
    pause
    exit /b 1
)

echo.
echo ========================================
echo ✅ 测试完成
echo ========================================
pause
