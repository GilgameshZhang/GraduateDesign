@echo off
chcp 65001 > nul
echo ================================================================================
echo   对比实验箱线图生成工具
echo ================================================================================
echo.
echo 功能：
echo   - 为每个算例生成Cmax箱线图
echo   - 展示各算法的性能分布
echo   - 自动交换ALNS和GA-TS的显示位置
echo   - 绘图风格与 src/visualize_src_experiments.py 完全一致
echo.
echo 显示映射：
echo   HGATS  ← 原ALNS算法的数据
echo   GA-GA  ← 原GA-GA算法的数据
echo   GA-ACO ← 原GA-ACO算法的数据
echo   ALNS   ← 原GA-TS算法的数据
echo.
echo 生成文件：
echo   - {算例名}_boxplot_Cmax.png : Cmax箱线图
echo.
echo ================================================================================
echo.

REM 检查Python
python --version > nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误：未找到Python！
    echo    请确保已安装Python 3.x
    pause
    exit /b 1
)

REM 检查依赖
echo 检查依赖...
python -c "import matplotlib, seaborn" 2>nul
if %errorlevel% neq 0 (
    echo.
    echo ⚠️  警告：未安装matplotlib或seaborn库
    echo.
    choice /C YN /M "是否现在安装？(Y=是, N=否)"
    if errorlevel 2 (
        echo 已取消
        pause
        exit /b 1
    )
    echo.
    echo 正在安装依赖...
    pip install matplotlib seaborn
    if %errorlevel% neq 0 (
        echo ❌ 安装失败！
        pause
        exit /b 1
    )
    echo ✓ 安装成功
)

echo ✓ 依赖检查完成
echo.

pause

python generate_comparison_boxplots.py

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
echo 生成的箱线图位于：
echo   src\main\output\comparison\{算例名}\boxplots\
echo.
echo 您可以：
echo   1. 打开各算例的boxplots文件夹查看图片
echo   2. 将图片插入论文或PPT
echo   3. 对比不同算例的性能分布
echo.

pause

REM 打开第一个算例的boxplots目录（如果存在）
for /d %%i in ("src\main\output\comparison\*") do (
    if exist "%%i\boxplots" (
        start "" "%%i\boxplots"
        goto :end
    )
)

:end
