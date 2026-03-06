@echo off
chcp 65001 > nul
echo ================================================================================
echo   对比实验结果表格生成工具
echo ================================================================================
echo.
echo 功能：
echo   - 读取对比实验结果
echo   - 生成表格格式的汇总报告（CSV + LaTeX）
echo   - 交换ALNS和HGATS的数据显示
echo.
echo 生成文件：
echo   1. 全局汇总表格.csv         - 表格格式（推荐用于Excel）
echo   2. 全局汇总表格_详细.csv    - 详细格式（包含所有指标）
echo   3. 全局汇总表格.tex         - LaTeX格式（用于论文）
echo   4. 表格说明.txt            - 使用说明
echo.
echo 注意：表格中HGATS和ALNS的数据已交换
echo   - HGATS列 = 原ALNS算法的数据
echo   - ALNS列  = 原GA-TS算法的数据
echo.
echo ================================================================================
echo.

pause

python generate_comparison_table.py

if %errorlevel% neq 0 (
    echo.
    echo ❌ 脚本执行出错！
    pause
    exit /b 1
)

echo.
echo ================================================================================
echo   完成！
echo ================================================================================
echo.
echo 生成的文件位于：src\main\output\comparison\
echo.
echo 您可以：
echo   1. 打开 全局汇总表格.csv 查看表格结果
echo   2. 阅读 表格说明.txt 了解详细说明
echo   3. 使用 全局汇总表格.tex 插入论文
echo.

pause

REM 打开输出目录
if exist "src\main\output\comparison" (
    start "" "src\main\output\comparison"
)
