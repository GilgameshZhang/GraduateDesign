@echo off
chcp 65001 >nul
echo ================================================================================
echo                    田口实验（GA-TS 参数校准）
echo ================================================================================
echo 因素：种群数量、交叉概率、变异概率、局部搜索次数、禁忌搜索迭代次数、领域大小
echo 算例：2M80J-01（2台机器，80工件，实例01）
echo 设计：L27 正交表，27 个试验号，每号 10 次重复，每次 5 分钟
echo 多线程运行（建议 3 线程，避免内存不足）
echo ================================================================================
echo.
echo 结果将保存到：chapter-1\src\main\output\田口实验\
echo   - 每个试验号：田口实验\TrialXX\2M80J_runN_时间戳\实验报告.txt（与消融一致）
echo   - 汇总：taguchi_detailed_results.csv, taguchi_summary_statistics.csv
echo   - 全局：taguchi_global_summary.csv, taguchi_report.txt, taguchi_L27_table.csv
echo.
echo 预计总耗时：约 7~8 小时（27*10*5min / 3 线程）
echo.
echo 正在启动田口实验（JVM 建议 -Xmx8g）...
cd /d "%~dp0"
mvn exec:java -Dexec.mainClass="TaguchiExperiment" -Dexec.classpathScope=test -Dexec.jvmArgs="-Xmx8g -Xms2g"
echo.
echo 田口实验结束。
pause
