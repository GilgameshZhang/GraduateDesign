@echo off
chcp 65001 > nul
echo ========================================
echo 测试禁忌搜索优化
echo ========================================
echo.
echo 本测试验证禁忌搜索对打印机零件排布的优化效果
echo.
echo 禁忌搜索参数：
echo - 最大迭代次数：50
echo - 邻域搜索次数：20
echo - 最小禁忌表长度：5
echo.
echo 优化目标：
echo 1. 减少打印分批数
echo 2. 提高材料利用率
echo 3. 降低总打印时间
echo.
echo ========================================
echo 开始测试...
echo ========================================
echo.

cd /d "%~dp0"

echo 测试实例：J20P3B2D5_01.txt
echo 参数：100代，种群100，交叉率0.8，变异率0.1
echo.

mvn exec:java -Dexec.mainClass="ProgramEntity.Main" -Dexec.args="src/main/resources/instance/J20P3B2D5_01.txt 100 100 0.8 0.1" -q

echo.
echo ========================================
echo 测试完成！
echo ========================================
echo.
echo 检查要点：
echo 1. 启动时应显示"🔍 禁忌搜索已启用"
echo 2. makespan应该比不使用禁忌搜索时更小
echo 3. 收敛速度应该更快
echo.
echo 提示：
echo - 如需禁用禁忌搜索，请修改 GA.java 中的 enableTabuSearch 参数
echo - 如需调整禁忌搜索参数，请修改 tabuMaxIterations 等参数
echo.
pause

