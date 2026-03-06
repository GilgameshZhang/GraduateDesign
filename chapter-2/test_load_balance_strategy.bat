@echo off
chcp 65001 > nul
echo ========================================
echo 测试打印机负载均衡初始化策略
echo ========================================
echo.
echo 本测试验证新增的负载均衡初始化策略
echo.
echo 【负载均衡策略说明】
echo.
echo 1. 核心原理：Round-Robin（轮流分配）
echo    - 将打印件依次轮流分配给所有打印机
echo    - 公式：machine = (i %% printMachineCount) + 1
echo.
echo 2. 优势：
echo    ✓ 简单高效（O(1)时间复杂度）
echo    ✓ 绝对均衡（每台机器最多相差1个零件）
echo    ✓ 减少瓶颈（避免某台机器过载）
echo    ✓ 确定性强（同样的零件顺序产生同样的分配）
echo.
echo 3. 分配示例（5个零件，3台打印机）：
echo    零件0 → M1, 零件1 → M2, 零件2 → M3
echo    零件3 → M1, 零件4 → M2
echo    结果：M1(2个), M2(2个), M3(1个) ← 几乎完全均衡
echo.
echo ========================================
echo 【可用的负载均衡策略】
echo ========================================
echo.
echo 1. loadBalanceStrategy()
echo    - 零件：随机排序
echo    - 打印机：负载均衡
echo    - 离散机：轮盘赌
echo.
echo 2. heightBalanceStrategy()
echo    - 零件：按高度降序
echo    - 打印机：负载均衡
echo    - 离散机：轮盘赌
echo    推荐：高零件优先 + 均匀分配 → 减少分批 + 避免瓶颈
echo.
echo 3. areaBalanceStrategy()
echo    - 零件：按面积降序
echo    - 打印机：负载均衡
echo    - 离散机：轮盘赌
echo    推荐：大零件优先 + 均匀分配 → 提高利用率 + 避免瓶颈
echo.
echo ========================================
echo 【与其他策略对比】
echo ========================================
echo.
echo 随机策略（RANDOM）：
echo - 负载分布：随机，可能不均衡
echo - 多样性：高
echo - 适用场景：探索性搜索
echo.
echo 轮盘赌策略（ROULETTE_WHEEL）：
echo - 负载分布：偏向快机器，可能不均衡
echo - 多样性：中
echo - 适用场景：利用机器差异
echo.
echo 负载均衡策略（LOAD_BALANCE）：
echo - 负载分布：绝对均衡 ✓
echo - 多样性：低
echo - 适用场景：初始均衡起点 ✓
echo.
echo ========================================
echo 【推荐使用场景】
echo ========================================
echo.
echo 1. 初始种群生成：
echo    30%% 负载均衡 + 40%% 轮盘赌 + 30%% 随机
echo    → 均衡起点 + 探索优秀 + 保持多样性
echo.
echo 2. 问题规模大时：
echo    负载均衡可以快速提供较好的初始解
echo.
echo 3. 机器能力相近时：
echo    负载均衡效果接近最优分配
echo.
echo ========================================
echo 开始测试...
echo ========================================
echo.

cd /d "%~dp0"

echo 测试1: 纯负载均衡策略
echo 实例：J20P3B2D5_01.txt
echo 参数：50代，种群50，交叉率0.8，变异率0.1
echo 预期：初始解质量较好，负载均衡
echo.

REM mvn exec:java -Dexec.mainClass="ProgramEntity.Main" ^
REM   -Dexec.args="src/main/resources/instance/J20P3B2D5_01.txt 50 50 0.8 0.1 loadBalance" -q

echo.
echo 测试2: 高度+负载均衡策略
echo 实例：J20P3B2D5_01.txt
echo 参数：50代，种群50，交叉率0.8，变异率0.1
echo 预期：高零件优先 + 均匀分配，效果更好
echo.

REM mvn exec:java -Dexec.mainClass="ProgramEntity.Main" ^
REM   -Dexec.args="src/main/resources/instance/J20P3B2D5_01.txt 50 50 0.8 0.1 heightBalance" -q

echo.
echo ========================================
echo 测试说明
echo ========================================
echo.
echo 注意：以上测试命令已注释，因为需要先在Main类中
echo       添加对初始化策略参数的支持。
echo.
echo 手动测试方法：
echo 1. 在GA.java中设置初始化策略：
echo    strategy = InitializationStrategy.loadBalanceStrategy();
echo.
echo 2. 运行算法：
echo    mvn exec:java -Dexec.mainClass="ProgramEntity.Main" ^
echo      -Dexec.args="src/main/resources/instance/J20P3B2D5_01.txt 50 50 0.8 0.1"
echo.
echo 3. 观察结果：
echo    - 初始代的makespan应该较好
echo    - 各打印机的零件数应该接近
echo    - 收敛速度应该较快
echo.
echo ========================================
echo 检查要点
echo ========================================
echo.
echo 1. 初始解质量
echo    - 第0代的平均makespan
echo    - 与随机/轮盘赌策略对比
echo.
echo 2. 负载均衡效果
echo    - 检查Gantt图中各打印机的工作量
echo    - 理想情况：各机器完工时间接近
echo.
echo 3. 收敛速度
echo    - 前10代的改善幅度
echo    - 是否比其他策略更快达到好解
echo.
echo 4. 最优解质量
echo    - 最终makespan
echo    - 与其他策略对比
echo.
echo ========================================
echo 性能预期
echo ========================================
echo.
echo 相比随机策略：
echo - 初始解质量：⬆️ 20-30%%
echo - 收敛速度：⬆️ 15-25%%
echo - 最优解质量：⬆️ 5-10%%
echo.
echo 相比轮盘赌策略：
echo - 初始解质量：接近（可能略优）
echo - 收敛速度：接近
echo - 最优解质量：接近
echo - 负载均衡：✓ 更好
echo.
echo 最佳实践：
echo - 混合使用多种策略（30%% 负载均衡 + 70%% 其他）
echo - 与排序策略结合（heightBalance或areaBalance）
echo - 根据问题特点调整比例
echo.
pause

