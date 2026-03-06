@echo off
echo 快速编译测试...

cd /d %~dp0

set CLASSPATH=target\classes;target\dependency\*

echo 编译 ScheduleVisualizer.java...
javac -cp "%CLASSPATH%" src/main/java/AlgorthmFrame/visualization/ScheduleVisualizer.java

if %errorlevel% equ 0 (
    echo ✓ ScheduleVisualizer.java 编译成功！
) else (
    echo ✗ ScheduleVisualizer.java 编译失败！
)

echo.
echo 编译 PrinterLayoutVisualizer.java...
javac -cp "%CLASSPATH%" src/main/java/AlgorthmFrame/visualization/PrinterLayoutVisualizer.java

if %errorlevel% equ 0 (
    echo ✓ PrinterLayoutVisualizer.java 编译成功！
) else (
    echo ✗ PrinterLayoutVisualizer.java 编译失败！
)

echo.
pause
