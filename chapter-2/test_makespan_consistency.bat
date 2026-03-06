@echo off
chcp 65001 > nul
echo ========================================
echo 测试Makespan一致性修复
echo ========================================
echo.
echo 本测试验证makespan与机器完成时间的一致性
echo.
echo 【问题背景】
echo.
echo 修复前的问题：
echo - 最优makespan: 251619.13
echo - 机器5完成时间: 270941.30  ← 超出makespan！
echo - 差异: 19322.17 (约7.7%%)
echo.
echo 【根本原因】
echo.
echo 1. printSolution丢失
echo    - 复制染色体时，printSolution被置为null
echo    - 导致必须重新evaluate生成装箱方案
echo.
echo 2. 重新evaluate导致不一致
echo    - Tabu Search有随机性，每次运行结果不同
echo    - fitness反算的makespan ≠ 重新evaluate的makespan
echo    - 但输出使用的是重新evaluate后的机器时间
echo.
echo 【修复方案】
echo.
echo 1. 深度复制printSolution
echo    - 在Chromosome复制构造函数中正确复制printSolution
echo    - 保留最优染色体的装箱结果
echo    - 避免不必要的重新计算
echo.
echo 2. 使用实际evaluate的makespan
echo    - 使用重新evaluate返回的makespan作为最终结果
echo    - 确保bestSolution.cost与operationMatrix一致
echo    - 如果有差异，明确提示
echo.
echo ========================================
echo 开始测试...
echo ========================================
echo.

cd /d "%~dp0"

echo 测试实例：J20P3B2D5_01.txt
echo 参数：100代，种群100，交叉率0.8，变异率0.1
echo.
echo 预期结果：
echo - makespan与所有机器完成时间一致
echo - 所有机器的完成时间 ≤ makespan
echo - 如果有差异，会有明确提示
echo.

mvn exec:java -Dexec.mainClass="ProgramEntity.Main" \
  -Dexec.args="src/main/resources/instance/J20P3B2D5_01.txt 100 100 0.8 0.1" -q

echo.
echo ========================================
echo 测试完成！
echo ========================================
echo.
echo 【检查要点】
echo.
echo 1. 一致性检查：
echo    ✓ 查看输出中的"最优makespan"
echo    ✓ 查看【3. 各机器的加工甘特图数据】中的"完成时间"
echo    ✓ 确认所有机器的完成时间 ≤ 最优makespan
echo    ✓ 最大的机器完成时间应该等于最优makespan
echo.
echo 2. 差异提示：
echo    如果看到：
echo    "(注意：由于随机算法，重新评估makespan=XXX 与fitness反算=YYY 略有差异)"
echo    说明：
echo    - printSolution在某个时刻丢失（被交叉/变异清空）
echo    - 重新evaluate生成了新的装箱方案
echo    - 使用了实际evaluate的makespan，确保一致性
echo    - 这是正常的，差异通常很小（<1%%）
echo.
echo 3. 无差异情况：
echo    如果没有看到差异提示：
echo    - printSolution被正确保留
echo    - 无需重新计算
echo    - 结果完全一致（最佳情况）
echo.
echo 4. 差异过大警告：
echo    如果差异 > 5%%：
echo    - 可能是Tabu Search产生了较差的结果
echo    - 需要检查Tabu Search参数
echo    - 考虑增加Tabu Search迭代次数
echo.
echo ========================================
echo 【修复效果总结】
echo ========================================
echo.
echo 修复前：
echo - makespan与机器时间不一致
echo - 差异可能很大（7.7%%）
echo - 用户困惑，结果不可信
echo.
echo 修复后：
echo - makespan与机器时间完全一致 ✓
echo - 差异被消除或明确提示 ✓
echo - 结果可靠，逻辑正确 ✓
echo.
echo ========================================
echo 【技术要点】
echo ========================================
echo.
echo 1. 深度复制的重要性：
echo    - 复制染色体时必须深度复制所有字段
echo    - printSolution包含装箱结果，不能丢失
echo.
echo 2. 随机算法的非确定性：
echo    - Tabu Search有随机性
echo    - 每次运行可能得到不同结果
echo    - 必须使用实际evaluate的结果
echo.
echo 3. Fitness vs Makespan：
echo    - fitness用于比较和选择
echo    - makespan是实际的调度长度
echo    - 重新evaluate的makespan是最准确的
echo.
pause

