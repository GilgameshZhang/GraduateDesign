@echo off
chcp 65001 > nul
echo ========================================
echo 测试初始化策略优化
echo ========================================
echo.
echo 本测试验证多样化初始化策略的效果
echo.
echo 初始化策略包括：
echo 1. 完全随机策略（20%%）
echo 2. 按高度降序+轮盘赌选择（20%%）
echo 3. 按面积降序+轮盘赌选择（20%%）
echo 4. 按高度降序+随机机器（20%%）
echo 5. 混合策略（20%%）
echo.
echo 预期效果：
echo - 初始种群质量提升15-25%%
echo - 收敛速度提升20-30%%
echo - 最优解出现更早
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
echo.
echo 1. 初始化信息
echo    应显示：[初始化] 生成多样化初始种群
echo    包含5种策略各20个个体
echo.
echo 2. 初始种群统计
echo    - 最优makespan：应该比纯随机低10-20%%
echo    - 平均makespan：应该更稳定
echo    - 最优/最差差距：应该更小
echo.
echo 3. 收敛速度
echo    - 前10代应该有明显改善
echo    - 达到目标解的代数应该减少
echo.
echo 4. 轮盘赌选择
echo    - 打印机选择基于printH和recoatingTime
echo    - 离散机器选择基于加工时长
echo    - 能力强的机器被选中概率更大
echo.
echo ========================================
echo 对比建议：
echo ========================================
echo.
echo 可以与之前的随机初始化结果对比：
echo - 初始种群质量差异
echo - 收敛速度差异
echo - 最终解质量差异
echo.
pause

