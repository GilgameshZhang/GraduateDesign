@echo off
chcp 65001 >nul
echo ================================================================
echo 测试operationMatrix修复
echo ================================================================
echo.
echo 本测试验证修复后：
echo 1. operationMatrix包含的是best染色体的调度数据
echo 2. 机器完成时间与best的makespan一致
echo 3. 对比fitness反算值和重新evaluate的值
echo.
echo ----------------------------------------------------------------
echo 编译项目...
echo ----------------------------------------------------------------
cd /d "%~dp0"
call mvn clean compile -DskipTests
if errorlevel 1 (
    echo 编译失败！
    pause
    exit /b 1
)

echo.
echo ----------------------------------------------------------------
echo 运行测试算例（小规模，10代）...
echo ----------------------------------------------------------------
call mvn exec:java -Dexec.mainClass="ProblemFrame.Main" -Dexec.args="src/main/resources/instance/J20P3B2D5_01.txt 10" -Dexec.cleanupDaemonThreads=false

echo.
echo ================================================================
echo 测试完成
echo ================================================================
echo.
echo 检查点：
echo 1. 查看"正在生成最优解的详细调度数据..."的提示
echo 2. 查看重新evaluate的makespan是否合理
echo 3. 查看【3. 各机器的加工甘特图数据】中的完成时间
echo 4. 对比"best schedule cost"和各机器的最大完成时间
echo 5. 它们应该基本一致（允许小误差）
echo.
pause

