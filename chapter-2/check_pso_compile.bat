@echo off
echo ================================================================================
echo                     PSO算法编译检查
echo ================================================================================
echo.

cd /d "%~dp0"

echo [1/4] 检查Maven环境...
call mvn --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Maven未安装或未配置环境变量
    echo 请安装Maven或使用IDE编译
    pause
    exit /b 1
)
echo ✓ Maven环境正常
echo.

echo [2/4] 清理旧的编译文件...
call mvn clean >nul 2>&1
echo ✓ 清理完成
echo.

echo [3/4] 编译PSO算法相关类...
echo 正在编译，请稍候...
call mvn compile 2>&1 | findstr /C:"ERROR" /C:"成功" /C:"SUCCESS" /C:"失败" /C:"FAILURE"

if errorlevel 1 (
    echo.
    echo ❌ 编译失败！请查看详细错误信息：
    call mvn compile
    pause
    exit /b 1
)

echo ✓ 编译成功
echo.

echo [4/4] 验证关键类文件...
set ERROR=0

if not exist "target\classes\AlgorthmFrame\pso\PSO.class" (
    echo ❌ PSO.class 未生成
    set ERROR=1
)

if not exist "target\classes\ProblemFrame\Particle.class" (
    echo ❌ Particle.class 未生成
    set ERROR=1
)

if not exist "target\classes\ProblemFrame\PSOParameters.class" (
    echo ❌ PSOParameters.class 未生成
    set ERROR=1
)

if %ERROR%==0 (
    echo ✓ 所有关键类文件已生成
    echo.
    echo ================================================================================
    echo                     ✅ 编译检查通过！
    echo ================================================================================
    echo.
    echo 现在可以运行:
    echo   1. TestPSO.java          - 快速测试PSO算法
    echo   2. PSOExperimentRunner.java - 批量运行PSO实验
    echo.
) else (
    echo.
    echo ❌ 部分类文件缺失，请检查编译错误
)

pause
