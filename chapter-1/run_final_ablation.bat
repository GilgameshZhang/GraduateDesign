@echo off
chcp 65001 >nul
echo ================================================================================
echo                          完整消融实验（多线程优化版）
echo ================================================================================
echo.
echo 消融4个关键组件：
echo   1. 初始化策略 - 50%%启发式 vs 0%%随机
echo   2. 变异算子 - LocalSearch(负载平衡) vs Simple(随机)
echo   3. 禁忌搜索 - TabuSearch vs NoSearch
echo   4. 评分策略 - Complex(多指标) vs Simple(单指标)
echo.
echo 实验配置：
echo   - 测试算例：5个不同规模
echo   - 配置数量：6个（V0-Full基线 + 5个消融版本）
echo   - 每配置运行次数：10次
echo   - 时间限制：每次5分钟
echo   - 线程数：最多3个线程并行（内存优化）
echo   - JVM内存：8GB
echo   - 预计总耗时：约8-10小时（多线程加速）
echo.
echo 结果将保存到：chapter-1/src/main/output/final_ablation/
echo ================================================================================
echo.
pause

echo 正在启动消融实验（增加JVM内存到8GB）...
mvn exec:java -Dexec.mainClass="FinalAblationExperiment" -Dexec.classpathScope=test -Dexec.args="-Xmx8g -Xms2g"

echo.
echo ================================================================================
echo 实验完成！
echo ================================================================================
echo.
echo 结果文件：
echo   - chapter-1/src/main/output/final_ablation/global_summary.csv
echo   - chapter-1/src/main/output/final_ablation/average_rpd.csv
echo   - 各算例详细结果在对应的子目录中
echo.
pause
