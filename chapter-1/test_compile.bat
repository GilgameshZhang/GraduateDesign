@echo off
chcp 65001 >nul
echo 编译测试...
echo.

cd /d "%~dp0"

echo 编译 BatchGenomeAblation.java...
javac -encoding UTF-8 -d target -cp "src/main/java" src/main/java/AlgorithmFrame/machineChoice/ga/BatchGenomeAblation.java
if errorlevel 1 (
    echo ✗ BatchGenomeAblation.java 编译失败
    pause
    exit /b 1
)
echo ✓ BatchGenomeAblation.java 编译成功

echo.
echo 编译 SkyLinePackingAblation.java...
javac -encoding UTF-8 -d target -cp "src/main/java" src/main/java/AlgorithmFrame/bachSelect/skyLine/SkyLinePackingAblation.java
if errorlevel 1 (
    echo ✗ SkyLinePackingAblation.java 编译失败
    pause
    exit /b 1
)
echo ✓ SkyLinePackingAblation.java 编译成功

echo.
echo 编译 BatchGaAblation.java...
javac -encoding UTF-8 -d target -cp "src/main/java" src/main/java/AlgorithmFrame/machineChoice/ga/BatchGaAblation.java
if errorlevel 1 (
    echo ✗ BatchGaAblation.java 编译失败
    pause
    exit /b 1
)
echo ✓ BatchGaAblation.java 编译成功

echo.
echo 编译 QuickAblationTest.java...
javac -encoding UTF-8 -d target -cp "src/main/java;src/test/java" src/test/java/QuickAblationTest.java
if errorlevel 1 (
    echo ✗ QuickAblationTest.java 编译失败
    pause
    exit /b 1
)
echo ✓ QuickAblationTest.java 编译成功

echo.
echo 编译 AblationExperimentRunner.java...
javac -encoding UTF-8 -d target -cp "src/main/java;src/test/java" src/test/java/AblationExperimentRunner.java
if errorlevel 1 (
    echo ✗ AblationExperimentRunner.java 编译失败
    pause
    exit /b 1
)
echo ✓ AblationExperimentRunner.java 编译成功

echo.
echo ========================================
echo 所有文件编译成功！
echo ========================================
pause
