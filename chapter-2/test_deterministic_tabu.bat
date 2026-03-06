@echo off
chcp 65001 >nul
echo ============================================
echo   测试确定性禁忌搜索
echo ============================================
echo.

echo [1] 编译项目...
call mvn clean compile -DskipTests
if errorlevel 1 (
    echo 编译失败！
    pause
    exit /b 1
)
echo.

echo [2] 运行GA算法（使用确定性禁忌搜索）...
call mvn exec:java -Dexec.mainClass="test.RunGA" -Dexec.args="src/main/resources/instance/J20P3B2D5_01.txt 30"
echo.

echo ============================================
echo   测试完成
echo ============================================
echo.
echo 请检查：
echo 1. 编译是否成功（无错误）
echo 2. 每次运行同一染色体是否得到相同结果
echo 3. 最优makespan是否稳定
echo.

pause

