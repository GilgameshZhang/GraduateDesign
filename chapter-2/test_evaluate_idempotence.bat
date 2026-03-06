@echo off
chcp 65001 > nul
echo ========================================
echo       evaluate幂等性测试
echo ========================================
echo.
echo 此测试将验证evaluate方法的幂等性：
echo - 相同的染色体多次调用evaluate应该得到相同的makespan
echo - 染色体的基因不应该被evaluate修改
echo.

cd /d "%~dp0"

echo [1/3] 清理旧的编译文件...
call mvn clean -q
if errorlevel 1 (
    echo ❌ 清理失败
    pause
    exit /b 1
)

echo [2/3] 编译项目...
call mvn compile -DskipTests -q
if errorlevel 1 (
    echo ❌ 编译失败
    pause
    exit /b 1
)
echo ✓ 编译成功

echo.
echo [3/3] 运行幂等性测试...
echo ========================================
echo.

call mvn exec:java -Dexec.mainClass="test.TestEvaluateIdempotence" -Dexec.classpathScope=test -q

if errorlevel 1 (
    echo.
    echo ❌ 测试执行失败
) else (
    echo.
    echo ✓ 测试执行完成
)

echo.
echo ========================================
echo 测试完成
echo ========================================
pause


