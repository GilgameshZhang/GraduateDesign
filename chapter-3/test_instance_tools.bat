@echo off
echo ============================================================
echo 第三章算例工具测试脚本
echo ============================================================
echo.

echo [1/4] 编译Java生成器...
cd chapter-3
if not exist "bin" mkdir bin

javac -encoding UTF-8 -d bin -sourcepath src/main/java ^
    src/main/java/util/java/EnergyAwareInstanceGenerator.java ^
    src/main/java/ProgramEntity/*.java ^
    src/main/java/ProblemFrame/*.java

if errorlevel 1 (
    echo 编译失败！
    pause
    exit /b 1
)
echo ✓ 编译成功
echo.

echo [2/4] 生成测试算例...
java -cp bin util.java.EnergyAwareInstanceGenerator

if errorlevel 1 (
    echo 生成失败！
    pause
    exit /b 1
)
echo ✓ 生成成功
echo.

echo [3/4] 验证测试算例...
python validate_instance.py src/main/resources/instance/test_J5P2B1D0_01.txt

if errorlevel 1 (
    echo 验证失败！
    pause
    exit /b 1
)
echo.

echo [4/4] 批量验证生成的算例...
python validate_instance.py --batch src/main/resources/instance/

echo.
echo ============================================================
echo 所有测试完成！
echo ============================================================
echo.
echo 生成的算例位置: chapter-3/src/main/resources/instance/
echo.
echo 下一步:
echo   1. 查看算例文件
echo   2. 在你的MOGWO算法中使用EnergyAwareInput读取算例
echo   3. 使用EnergyCalculator计算能耗
echo.
pause
