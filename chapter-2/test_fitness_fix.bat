@echo off
chcp 65001 > nul
echo ========================================
echo 测试适应度计算修复
echo ========================================
echo.
echo 本测试验证以下问题的修复：
echo 1. 适应度缩放系数一致性问题
echo 2. best个体能够正确更新
echo 3. makespan持续优化
echo.
echo 预期结果：
echo - best fitness应该会逐代提升（数值变大）
echo - best makespan应该会逐代降低（数值变小）
echo - 当找到更好的解时，会打印 "find new best fitness"
echo.
echo ========================================
echo 开始测试...
echo ========================================
echo.

cd /d "%~dp0"

REM 使用小规模实例进行快速测试
mvn exec:java -Dexec.mainClass="ProgramEntity.Main" -Dexec.args="src/main/resources/instance/J20P3B2D5_01.txt 200 30 0.8 0.2" -q

echo.
echo ========================================
echo 测试完成！请检查：
echo 1. best fitness是否在增长
echo 2. best makespan是否在降低
echo 3. 是否出现 "find new best fitness" 消息
echo ========================================
pause

