@echo off
chcp 65001 > nul
echo ========================================
echo 测试统计数据一致性修复
echo ========================================
echo.
echo 本测试验证以下问题的修复：
echo 1. min makespan不会小于best makespan
echo 2. 统计数据基于fitness反算，不重复evaluate
echo 3. 数据一致性验证机制
echo.
echo 预期结果：
echo - 当代的min makespan = best makespan（如果best是当代最优）
echo - 不会出现"min ^< best"的矛盾现象
echo - 如果有不一致，会打印警告信息
echo.
echo ========================================
echo 开始测试...
echo ========================================
echo.

cd /d "%~dp0"

REM 使用小规模实例进行快速测试
echo 测试实例：J20P3B2D5_01.txt
echo 参数：200代，种群30，交叉率0.8，变异率0.2
echo.

mvn exec:java -Dexec.mainClass="ProgramEntity.Main" -Dexec.args="src/main/resources/instance/J20P3B2D5_01.txt 200 30 0.8 0.2" -q

echo.
echo ========================================
echo 测试完成！请检查：
echo ========================================
echo.
echo 1. 在输出中查找 "find new best fitness" 行
echo    示例：In X generation, find new best fitness is: Y, makespan: Z
echo.
echo 2. 对比同一代的统计信息
echo    示例：After X generation, best: Y (makespan: Z), min makespan: M
echo.
echo 3. 验证：Z 应该 ^<= M （best makespan 应该小于等于 min makespan）
echo    如果出现 Z ^> M 的情况，说明仍有问题！
echo.
echo 4. 查找是否有 "⚠️ 警告：统计数据不一致" 消息
echo    如果没有警告，说明数据一致性检查通过！
echo.
echo ========================================
echo 关键指标说明：
echo ========================================
echo.
echo • best fitness：全局最优个体的适应度（越大越好）
echo • best makespan：全局最优个体的完工时间（越小越好）
echo • avg makespan：当代所有个体的平均完工时间
echo • min makespan：当代最优个体的完工时间
echo.
echo 正常情况下：
echo   - best fitness 应该逐代增长（或保持不变）
echo   - best makespan 应该逐代降低（或保持不变）
echo   - min makespan ^>= best makespan（当代最优不会比历史最优更差）
echo.
echo 如果 min makespan ^< best makespan，说明：
echo   - 当代找到了更好的解，但best没有被更新
echo   - 或者统计数据计算不一致（修复前的问题）
echo.
echo ========================================
pause

