@echo off
chcp 65001 > nul
echo ========================================
echo       遗传算法迭代曲线图测试
echo ========================================
echo.
echo 此测试将运行遗传算法并生成迭代曲线图
echo 迭代曲线图显示每一代的最优makespan变化趋势
echo.

cd /d "%~dp0"

echo [1/2] 编译项目...
call mvn clean compile -DskipTests -q
if errorlevel 1 (
    echo ❌ 编译失败
    pause
    exit /b 1
)
echo ✓ 编译成功
echo.

echo [2/2] 运行算法（生成迭代曲线图）...
echo ========================================
echo.

call mvn exec:java -Dexec.mainClass="test.RunAlgorithmExample" -q

if errorlevel 1 (
    echo.
    echo ❌ 测试执行失败
) else (
    echo.
    echo ========================================
    echo ✓ 测试完成
    echo ========================================
    echo.
    echo 生成的图表文件：
    echo   1. visualization_results\gantt_chart.png - 甘特图
    echo   2. visualization_results\printer_layout.png - 打印机布局图
    echo   3. visualization_results\iteration_curve.png - 迭代曲线图（新增）
    echo   4. visualization_results\summary_report.json - 汇总报告
    echo.
    echo 请查看 visualization_results\iteration_curve.png 查看迭代曲线
    echo.
    echo 迭代曲线图说明：
    echo   - 横轴：迭代次数（代数），从0到200
    echo   - 纵轴：最优Makespan值
    echo   - 曲线：蓝色实线，显示makespan随迭代变化的趋势
    echo   - 数据点：每个蓝色圆点代表一代的最优makespan
    echo.
)

pause

