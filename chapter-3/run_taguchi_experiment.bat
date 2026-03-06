@echo off
chcp 65001 >nul
echo ═══════════════════════════════════════════════════════════════
echo     NSGA-II 田口实验运行器
echo ═══════════════════════════════════════════════════════════════
echo.
echo 正在编译项目...
echo.

cd /d "%~dp0"

REM 编译项目
call mvn clean compile -q

if %errorlevel% neq 0 (
    echo.
    echo ❌ 编译失败！请检查代码错误
    pause
    exit /b 1
)

echo ✅ 编译成功
echo.
echo 正在运行田口实验...
echo.
echo 注意：
echo   - 实验将运行L9正交表（9组参数配置）
echo   - 每组参数运行10次
echo   - 预计耗时：20-30分钟（取决于算例大小）
echo   - 结果保存在 chapter-3\output\taguchi_experiment\ 目录
echo.

REM 运行田口实验
call mvn test -Dtest=NSGAIITaguchiExperimentRunner -q

if %errorlevel% neq 0 (
    echo.
    echo ❌ 运行失败！请检查错误信息
    pause
    exit /b 1
)

echo.
echo ═══════════════════════════════════════════════════════════════
echo     实验完成！
echo ═══════════════════════════════════════════════════════════════
echo.
echo 结果文件位置：
echo   chapter-3\output\taguchi_experiment\
echo.
echo 包含内容：
echo   - L9实验总结报告.txt （详细分析报告）
echo   - 因子效应分析
echo   - 推荐参数配置
echo   - 实验结果排名
echo.

pause
