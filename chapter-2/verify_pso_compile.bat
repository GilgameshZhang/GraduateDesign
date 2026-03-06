@echo off
echo ================================================================================
echo                     PSO算法编译验证
echo ================================================================================
echo.

cd /d "%~dp0"

echo [检查] 正在编译PSO相关类...
echo.

REM 使用javac直接编译关键类（如果Maven不可用）
echo 方式1: 使用Maven编译
echo.

call mvn compile -DskipTests 2>&1 | findstr /C:"BUILD SUCCESS" /C:"BUILD FAILURE" /C:"ERROR" /C:"错误"

if errorlevel 1 (
    echo.
    echo ❌ 编译可能存在问题，显示详细信息：
    echo.
    call mvn compile -DskipTests
    echo.
    pause
    exit /b 1
) else (
    echo.
    echo ✅ 编译成功！
    echo.
)

echo [验证] 检查关键类文件...
echo.

set ALL_OK=1

if exist "target\classes\AlgorthmFrame\pso\PSO.class" (
    echo ✓ PSO.class 已生成
) else (
    echo ✗ PSO.class 缺失
    set ALL_OK=0
)

if exist "target\classes\ProblemFrame\Particle.class" (
    echo ✓ Particle.class 已生成
) else (
    echo ✗ Particle.class 缺失
    set ALL_OK=0
)

if exist "target\classes\ProblemFrame\PSOParameters.class" (
    echo ✓ PSOParameters.class 已生成
) else (
    echo ✗ PSOParameters.class 缺失
    set ALL_OK=0
)

if exist "target\classes\util\java\ExperimentResultWriter.class" (
    echo ✓ ExperimentResultWriter.class 已生成
) else (
    echo ✗ ExperimentResultWriter.class 缺失
    set ALL_OK=0
)

echo.

if %ALL_OK%==1 (
    echo ================================================================================
    echo                     ✅ 所有类编译成功！
    echo ================================================================================
    echo.
    echo 已修复的问题:
    echo   ✓ Solution类冲突 (使用具体导入)
    echo   ✓ writeIterationInfo参数不匹配 (添加String重载)
    echo.
    echo 现在可以运行:
    echo   1. TestPSO.main^(^)              - 快速测试 ^(5分钟^)
    echo   2. PSOExperimentRunner.main^(^)  - 批量实验 ^(1-2小时^)
    echo.
) else (
    echo ================================================================================
    echo                     ❌ 编译验证失败
    echo ================================================================================
    echo.
    echo 请检查上述缺失的类文件
    echo.
)

pause

