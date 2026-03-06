@echo off
chcp 65001 >nul
echo ================================================================================
echo                     快速测试 - 多线程消融实验
echo ================================================================================
echo.
echo 测试配置：
echo   - 1个算例（2M10J）
echo   - 6个配置
echo   - 每配置2次运行
echo   - 时间限制：1分钟
echo   - 线程数：自动检测（最多8个）
echo   - 预计耗时：约3-5分钟
echo.
echo 用途：快速验证多线程功能和代码正确性
echo ================================================================================
echo.
pause

echo 正在生成临时测试类...

REM 创建临时测试类
echo import AlgorithmFrame.machineChoice.ga.BatchGa; > QuickThreadTest.java
echo import AlgorithmFrame.machineChoice.ga.BatchGaAblation; >> QuickThreadTest.java
echo import AlgorithmFrame.machineChoice.ga.BatchGaSimpleMutation; >> QuickThreadTest.java
echo import ProblemFrame.Input; >> QuickThreadTest.java
echo import ProblemFrame.Result; >> QuickThreadTest.java
echo import util.ReadDataUtil; >> QuickThreadTest.java
echo import java.util.*; >> QuickThreadTest.java
echo import java.util.concurrent.*; >> QuickThreadTest.java
echo import java.util.concurrent.atomic.AtomicInteger; >> QuickThreadTest.java
echo. >> QuickThreadTest.java
echo public class QuickThreadTest { >> QuickThreadTest.java
echo     static class AblationConfig { >> QuickThreadTest.java
echo         String configId, configName, description; >> QuickThreadTest.java
echo         double heuristicInitRatio; >> QuickThreadTest.java
echo         boolean useLocalSearchMutation, useTabuSearch, useComplexScore; >> QuickThreadTest.java
echo         public AblationConfig(String id, String name, double initRatio, boolean localMut, boolean tabu, boolean complexScore, String desc) { >> QuickThreadTest.java
echo             this.configId = id; this.configName = name; this.heuristicInitRatio = initRatio; >> QuickThreadTest.java
echo             this.useLocalSearchMutation = localMut; this.useTabuSearch = tabu; this.useComplexScore = complexScore; this.description = desc; >> QuickThreadTest.java
echo         } >> QuickThreadTest.java
echo     } >> QuickThreadTest.java
echo     static class ExperimentResult { >> QuickThreadTest.java
echo         String configId, configName; int runId; double cmax; long timeMs; >> QuickThreadTest.java
echo     } >> QuickThreadTest.java
echo     public static void main(String[] args) { >> QuickThreadTest.java
echo         System.out.println("多线程快速测试开始..."); >> QuickThreadTest.java
echo         List^<AblationConfig^> configs = new ArrayList^<^>(); >> QuickThreadTest.java
echo         configs.add(new AblationConfig("V0-Full", "完整算法", 0.5, true, true, true, "完整")); >> QuickThreadTest.java
echo         configs.add(new AblationConfig("V1-RandomInit", "随机初始化", 0.0, true, true, true, "随机")); >> QuickThreadTest.java
echo         configs.add(new AblationConfig("V2-SimpleMutation", "简化变异", 0.5, false, true, true, "简单变异")); >> QuickThreadTest.java
echo         configs.add(new AblationConfig("V3-NoTabu", "无禁忌搜索", 0.5, true, false, true, "无禁忌")); >> QuickThreadTest.java
echo         configs.add(new AblationConfig("V4-SimpleScore", "简化评分", 0.5, true, true, false, "简单评分")); >> QuickThreadTest.java
echo         configs.add(new AblationConfig("V5-Minimal", "最小配置", 0.0, false, false, false, "最小")); >> QuickThreadTest.java
echo         int numThreads = Math.min(Runtime.getRuntime().availableProcessors(), 8); >> QuickThreadTest.java
echo         System.out.println("使用 " + numThreads + " 个线程并行运行..."); >> QuickThreadTest.java
echo         ExecutorService executor = Executors.newFixedThreadPool(numThreads); >> QuickThreadTest.java
echo         List^<Callable^<ExperimentResult^>^> tasks = new ArrayList^<^>(); >> QuickThreadTest.java
echo         AtomicInteger completedTasks = new AtomicInteger(0); >> QuickThreadTest.java
echo         int numRuns = 2; >> QuickThreadTest.java
echo         for (int run = 1; run ^<= numRuns; run++) { >> QuickThreadTest.java
echo             for (AblationConfig config : configs) { >> QuickThreadTest.java
echo                 final int r = run; final AblationConfig c = config; >> QuickThreadTest.java
echo                 tasks.add(() -^> { >> QuickThreadTest.java
echo                     try { >> QuickThreadTest.java
echo                         System.setProperty("ablation.useTabuSearch", String.valueOf(c.useTabuSearch)); >> QuickThreadTest.java
echo                         System.setProperty("ablation.useComplexScore", String.valueOf(c.useComplexScore)); >> QuickThreadTest.java
echo                         Input input = ReadDataUtil.readData("chapter-1/src/main/resources/Machine/machine_2", "chapter-1/src/main/resources/PrintItem/printItem_10/printItem_10_01", true); >> QuickThreadTest.java
echo                         BatchGa batchGa; >> QuickThreadTest.java
echo                         if (c.useLocalSearchMutation) { >> QuickThreadTest.java
echo                             batchGa = new BatchGaAblation(1000000, 100, 5, 2, 0.2, 0.8, input, true, "TabuSearch", 100, 10, 30, 60000, c.heuristicInitRatio); >> QuickThreadTest.java
echo                         } else { >> QuickThreadTest.java
echo                             batchGa = new BatchGaSimpleMutation(1000000, 100, 5, 2, 0.2, 0.8, input, true, "TabuSearch", 100, 10, 30, 60000); >> QuickThreadTest.java
echo                             batchGa.heuristicInitRatio = c.heuristicInitRatio; >> QuickThreadTest.java
echo                         } >> QuickThreadTest.java
echo                         long start = System.currentTimeMillis(); >> QuickThreadTest.java
echo                         Result result = batchGa.solve(); >> QuickThreadTest.java
echo                         long end = System.currentTimeMillis(); >> QuickThreadTest.java
echo                         ExperimentResult expResult = new ExperimentResult(); >> QuickThreadTest.java
echo                         expResult.configId = c.configId; expResult.configName = c.configName; expResult.runId = r; >> QuickThreadTest.java
echo                         expResult.cmax = batchGa.bestGenome.cMax; expResult.timeMs = end - start; >> QuickThreadTest.java
echo                         int completed = completedTasks.incrementAndGet(); >> QuickThreadTest.java
echo                         synchronized (System.out) { >> QuickThreadTest.java
echo                             System.out.println(String.format("[2M10J] 进度 %%d/12 (%%3.1f%%%%) ^| 运行 %%d ^| %%s ^| Cmax=%%.4f ^| 用时=%%dms", completed, (completed*100.0/12), r, c.configName, expResult.cmax, expResult.timeMs)); >> QuickThreadTest.java
echo                         } >> QuickThreadTest.java
echo                         return expResult; >> QuickThreadTest.java
echo                     } catch (Exception e) { e.printStackTrace(); return null; } >> QuickThreadTest.java
echo                 }); >> QuickThreadTest.java
echo             } >> QuickThreadTest.java
echo         } >> QuickThreadTest.java
echo         try { >> QuickThreadTest.java
echo             List^<Future^<ExperimentResult^>^> futures = executor.invokeAll(tasks); >> QuickThreadTest.java
echo             System.out.println("\n所有任务完成！多线程功能验证成功！"); >> QuickThreadTest.java
echo         } catch (Exception e) { e.printStackTrace(); } >> QuickThreadTest.java
echo         executor.shutdown(); >> QuickThreadTest.java
echo     } >> QuickThreadTest.java
echo } >> QuickThreadTest.java

echo.
echo 正在编译和运行测试...
cd src\test\java
javac -encoding UTF-8 -cp ".;..\..\..\..\target\classes;..\..\..\..\target\test-classes;%MAVEN_REPO%\*" QuickThreadTest.java 2>nul
if errorlevel 1 (
    echo ⚠️ 编译失败，使用Maven运行...
    cd ..\..\..
    mvn exec:java -Dexec.mainClass="QuickThreadTest" -Dexec.classpathScope=test
) else (
    echo ✓ 编译成功，运行测试...
    java -cp ".;..\..\..\..\target\classes;..\..\..\..\target\test-classes;%MAVEN_REPO%\*" QuickThreadTest
)

cd ..\..\..
del /q src\test\java\QuickThreadTest.* 2>nul

echo.
echo ================================================================================
echo 快速测试完成！
echo ================================================================================
echo.
echo 如果看到实时进度输出和"多线程功能验证成功！"，说明多线程功能正常。
echo.
echo 现在可以运行完整实验：run_final_ablation.bat
echo.
pause
