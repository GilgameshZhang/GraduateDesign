@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo 验证工序机器分配逻辑
echo ========================================
echo.

cd /d "%~dp0"

set SRC=chapter-3\src\main\java
set BIN=chapter-3\target\test-classes

echo [1/3] 清理旧的class文件...
if exist "%BIN%\util\java\VerifyOperationMachineAssignment.class" del "%BIN%\util\java\VerifyOperationMachineAssignment.class"

echo [2/3] 编译验证程序...
javac -encoding UTF-8 -d "%BIN%" -cp "%BIN%" ^
    "%SRC%\util\java\VerifyOperationMachineAssignment.java" ^
    "%SRC%\util\java\EnergyAwareInstanceGenerator.java" ^
    "%SRC%\ProblemFrame\PowerParameters.java"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ 编译失败！
    pause
    exit /b 1
)

echo ✅ 编译成功！
echo.
echo [3/3] 运行验证...
echo ----------------------------------------
java -cp "%BIN%" util.java.VerifyOperationMachineAssignment

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ 运行失败！
    pause
    exit /b 1
)

echo.
echo ========================================
echo ✅ 验证完成
echo ========================================
pause
