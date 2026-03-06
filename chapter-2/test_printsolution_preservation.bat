@echo off
chcp 65001 > nul
echo ========================================
echo 测试printSolution保留机制
echo ========================================
echo.
echo 本测试验证最优解的printSolution是否被正确保留
echo.
echo 【问题背景】
echo.
echo 修复前的问题：
echo - 第199代：best makespan = 251147.14
echo - 第200代输出：makespan = 269396.29
echo - 差异：18249.15 (7.3%%)  ← 不可接受
echo.
echo 原因：
echo 1. 交叉/变异清空printSolution（设计正确）
echo 2. best染色体的printSolution也被清空（意外副作用）
echo 3. 最后输出时重新evaluate
echo 4. Tabu Search随机性导致结果不同
echo.
echo 【修复方案】
echo.
echo 三层防护机制：
echo.
echo 防护层1：更新best时立即evaluate
echo - 当发现新的best时，立即对它evaluate
echo - 确保printSolution被保存
echo.
echo 防护层2：每代结束时检查
echo - 在每代结束时，检查best.printSolution
echo - 如果为null，立即重新evaluate
echo.
echo 防护层3：最后输出时明确提示
echo - 如果printSolution存在：显示"✓ 使用了保存的装箱结果"
echo - 如果printSolution丢失：显示"⚠️ 装箱结果丢失"
echo - 给出详细的差异信息和建议
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
echo - 迭代过程中的makespan与最终输出的makespan一致
echo - 或者差异很小（<1%%）
echo - 如果有差异，会有明确提示和原因
echo.

mvn exec:java -Dexec.mainClass="ProgramEntity.Main" \
  -Dexec.args="src/main/resources/instance/J20P3B2D5_01.txt 100 100 0.8 0.1" -q

echo.
echo ========================================
echo 测试完成！
echo ========================================
echo.
echo 【结果分析】
echo.
echo 场景1：完美情况（printSolution保留成功）
echo --------------------------------------------
echo 输出示例：
echo  After 99 generation, the best fitness is:XXX (makespan: 251147.14)
echo  After 100 generation, the best schedule cost is:251147.14
echo  ✓ 使用了保存的装箱结果，结果一致可靠
echo.
echo 分析：
echo - makespan完全一致 ✓
echo - printSolution被成功保留 ✓
echo - 结果最可靠 ✓✓✓
echo.
echo 场景2：差异很小（printSolution丢失但影响小）
echo --------------------------------------------
echo 输出示例：
echo  After 99 generation, the best fitness is:XXX (makespan: 251147.14)
echo  ⚠️ 检测到best.printSolution=null，重新evaluate以保存装箱结果
echo  After 100 generation, the best schedule cost is:251280.50
echo  ⚠️ 装箱结果丢失，重新生成导致差异：
echo     - 重新评估makespan: 251280.50
echo     - Fitness反算值: 251147.14
echo     - 差异: 133.36 (0.05%%)
echo.
echo 分析：
echo - 差异很小（<0.1%%）✓
echo - printSolution在某处丢失
echo - 防护层2捕获并修复
echo - 结果可接受 ✓✓
echo.
echo 场景3：差异较大（printSolution丢失且Tabu随机性大）
echo --------------------------------------------
echo 输出示例：
echo  After 99 generation, the best fitness is:XXX (makespan: 251147.14)
echo  ⚠️ 检测到best.printSolution=null，重新evaluate以保存装箱结果
echo  After 100 generation, the best schedule cost is:269396.29
echo  ⚠️ 装箱结果丢失，重新生成导致差异：
echo     - 重新评估makespan: 269396.29
echo     - Fitness反算值: 251147.14
echo     - 差异: 18249.15 (7.27%%)
echo     建议：检查交叉/变异是否过度清空printSolution
echo.
echo 分析：
echo - 差异较大（>5%%）⚠️
echo - printSolution丢失
echo - Tabu Search产生了较差结果
echo - 需要优化参数 ⚠️
echo.
echo ========================================
echo 【优化建议】
echo ========================================
echo.
echo 如果经常出现场景3（差异>5%%），可以尝试：
echo.
echo 1. 降低交叉/变异率
echo    crossoverRate = 0.6  (从0.8降低)
echo    mutationRate = 0.05  (从0.1降低)
echo    → 减少printSolution清空的频率
echo.
echo 2. 增加Tabu Search迭代次数
echo    tabuMaxIterations = 100  (从50增加)
echo    → 提高装箱质量，减少随机性影响
echo.
echo 3. 增加精英保留比例
echo    eliteCount = popSize * 0.2  (20%%)
echo    → 更多个体保留printSolution
echo.
echo 4. 使用固定种子（测试时）
echo    chromosome.r = new Random(12345)
echo    → 确保结果可重复
echo.
echo ========================================
echo 【技术要点】
echo ========================================
echo.
echo 1. printSolution的生命周期：
echo    - 创建：在evaluate时生成
echo    - 保留：通过Chromosome复制构造函数
echo    - 清空：在交叉/变异时（必要的）
echo    - 保护：三层防护机制
echo.
echo 2. 为什么必须清空？
echo    - printSolution对应特定的基因
echo    - 基因改变后，printSolution失效
echo    - 必须清空，避免使用过时数据
echo.
echo 3. 为什么有差异？
echo    - Tabu Search使用随机邻域生成
echo    - 每次运行可能得到不同装箱方案
echo    - 不同方案导致不同makespan
echo.
echo 4. 如何判断结果可靠？
echo    - 看到"✓ 使用了保存的装箱结果" → 最可靠
echo    - 差异<1%% → 可接受
echo    - 差异>5%% → 需要优化参数
echo.
echo ========================================
echo 【监控指标】
echo ========================================
echo.
echo 运行多次，观察：
echo.
echo 1. printSolution保留成功率
echo    - 出现"✓ 使用了保存的装箱结果"的次数
echo    - 目标：>80%%
echo.
echo 2. 平均差异
echo    - 所有运行的差异平均值
echo    - 目标：<1%%
echo.
echo 3. 最大差异
echo    - 所有运行的差异最大值
echo    - 目标：<3%%
echo.
echo 4. 差异>5%%的次数
echo    - 需要特别关注的情况
echo    - 目标：0次
echo.
pause

