@echo off
echo 编译可视化器类...

cd /d %~dp0

javac -cp "target/classes;target/dependency/*" src/main/java/AlgorthmFrame/visualization/ScheduleVisualizer.java

if %errorlevel% equ 0 (
    echo ✓ 编译成功！
) else (
    echo ✗ 编译失败！
)

pause
