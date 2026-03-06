import AlgorithmFrame.nsgaii.NSGAII;
import AlgorithmFrame.nsgaii.ParetoFrontVisualizer;
import AlgorithmFrame.visualization.ScheduleVisualizer;
import AlgorithmFrame.visualization.PrinterLayoutVisualizer;
import AlgorithmFrame.moead.MOEAD;
import AlgorithmFrame.mogwo.MOGWO;
import AlgorithmFrame.spea2.SPEA2;
import ProblemFrame.MOEvaluator;
import ProblemFrame.MOIndividual;
import ProblemFrame.Chromosome;
import ProblemFrame.CaculateFitness;
import ProblemFrame.Solution;
import ProgramEntity.EnergyAwareInput;
import ProgramEntity.Problem;
import ProgramEntity.Operation;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 算法对比实验运行器
 * 
 * 功能：
 * 1. 支持5种算法对比：NSGA-II、NSGA-II-Basic、MOEA/D、MOGWO、SPEA2
 * 2. 每个算例每个算法运行10次
 * 3. 计算三个核心指标：HV、IGD、C-metric
 * 4. 输出结构：对比试验包/算例名称/算法名称/运行次数/
 * 5. 汇总每个算法的帕累托前沿用于后续画图
 * 
 * 算法配置：
 * - NSGA-II:        启发式初始化 + 局部搜索 + 智能开关机策略（改进版）⭐
 * - NSGA-II-Basic:  标准初始化 + 无局部搜索 + 纯待机策略（基准版）
 * - MOEA/D:         标准初始化 + 无局部搜索 + 纯待机策略（基准版）
 * - MOGWO:          标准初始化 + 无局部搜索 + 纯待机策略（基准版）
 * - SPEA2:          标准初始化 + 无局部搜索 + 纯待机策略（基准版）
 * 
 * 能耗策略说明：
 * - 智能开关机策略：机器空闲时间超过阈值则关机，节省能耗（NSGA-II专用）
 * - 纯待机策略：机器空闲时保持待机状态，不关机（其他算法使用）
 * 
 * 实验目的：
 * 通过与基准算法对比，证明NSGA-II改进策略（局部搜索+开关机）的有效性
 * 
 * @author AI Assistant
 * @version 1.3.1
 */
public class AlgorithmComparisonExperimentRunner {
    
    // ==================== 实验配置 ====================
    
    /** 每个算法重复次数 */
    private static final int REPEAT_TIMES = 10;
    
    /** 最大运行时间（分钟） */
    private static final double MAX_RUN_TIME = 5;
    
    /** 最大代数 */
    private static final int MAX_GENERATIONS = 500000000;
    
    /** 种群大小 */
    private static final int POPULATION_SIZE = 150;
    
    /** 交叉率 */
    private static final double CROSSOVER_RATE = 0.9;
    
    /** 变异率 */
    private static final double MUTATION_RATE = 0.2;
    
    /** 是否启用局部搜索 */
    private static final boolean ENABLE_LOCAL_SEARCH = true;
    
    /** 局部搜索参数 - L（每个精英的尝试次数） */
    private static final int LOCAL_SEARCH_L = 20;
    
    /** 是否启用多线程 */
    private static final boolean ENABLE_MULTI_THREAD = true;
    
    /** 线程池大小（0表示自动检测CPU核心数） */
    private static final int THREAD_POOL_SIZE = 0;
    
    /** 输出目录 */
    private static final String OUTPUT_DIR = "chapter-3/output/comparison_experiment/";
    
    /** 参与对比的算法 */
    private static final String[] ALGORITHMS = {"NSGAII", "NSGAII-Basic", "MOEAD", "MOGWO", "SPEA2"};
    
    // ==================== 主程序 ====================
    
    public static void main(String[] args) {
        try {
            // 打印欢迎信息
            printWelcome();
            
            // 选择算例或批量运行
            runComparison();
            
            System.out.println("\n✅ 所有对比实验完成！");
            
        } catch (Exception e) {
            System.err.println("\n❌ 程序执行出错:");
            System.err.println("   " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 运行对比实验
     */
    private static void runComparison() throws Exception {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("请选择运行模式:");
        System.out.println("1. 单个算例对比");
        System.out.println("2. 批量算例对比（J20全部5个算例）");
        System.out.println("3. 批量算例对比（J50全部5个算例）");
        System.out.println("4. 批量算例对比（J100全部5个算例）");
        System.out.println("5. 全部算例对比");
        System.out.println("6. 批量算例对比（J10全部5个算例）");
        System.out.print("\n请输入选择 (1-5): ");
        
        String choice = scanner.nextLine().trim();
        
        List<String> instancePaths = new ArrayList<>();
        String basePath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-3\\src\\main\\resources\\instance\\";
        
        switch (choice) {
            case "1":
                // 单个算例
                String instancePath = selectInstance();
                instancePaths.add(instancePath);
                break;
            case "2":
                // J20全部
                for (int i = 1; i <= 5; i++) {
                    instancePaths.add(basePath + String.format("J20P3B2D5_%02d.txt", i));
                }
                break;
            case "3":
                // J50全部
                for (int i = 1; i <= 5; i++) {
                    instancePaths.add(basePath + String.format("J50P4B3D8_%02d.txt", i));
                }
                break;
            case "4":
                // J100全部
                for (int i = 1; i <= 5; i++) {
                    instancePaths.add(basePath + String.format("J100P5B4D10_%02d.txt", i));
                }
                break;
            case "5":
                // 全部算例
                for (int i = 1; i <= 5; i++) {
                    instancePaths.add(basePath + String.format("J20P3B2D5_%02d.txt", i));
                    instancePaths.add(basePath + String.format("J50P4B3D8_%02d.txt", i));
                    instancePaths.add(basePath + String.format("J100P5B4D10_%02d.txt", i));
                }
                break;
            case "6":
                // J10全部
                for (int i = 1; i <= 5; i++) {
                    instancePaths.add(basePath + String.format("J10P2B1D2_%02d.txt", i));
                }
                break;
            default:
                System.out.println("无效选择，使用默认算例");
                instancePaths.add(basePath + "J20P3B2D5_01.txt");
        }
        
        // 对每个算例运行对比实验
        for (String instancePath : instancePaths) {
            runSingleInstanceComparison(instancePath);
        }
    }
    
    /**
     * 运行单个算例的对比实验
     */
    private static void runSingleInstanceComparison(String instancePath) throws Exception {
        String instanceName = extractInstanceName(instancePath);
        
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║  算例: " + instanceName + "                                    ");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        
        // 加载算例
        Problem problem = loadProblem(instancePath);
        System.out.println("✅ 算例加载成功");
        System.out.println("   工件数: " + problem.getJobCount());
        System.out.println("   机器数: " + problem.getMachineCount());
        
        // 创建输出目录
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String instanceOutputDir = OUTPUT_DIR + instanceName + "_" + timestamp + "/";
        new File(instanceOutputDir).mkdirs();
        
        // 存储所有算法的结果
        Map<String, List<List<MOIndividual>>> allResults = new HashMap<>();
        Map<String, List<Double>> allRunTimes = new HashMap<>();
        
        // 运行所有算法
        for (String algorithm : ALGORITHMS) {
            System.out.println("\n════════════════════════════════════════════════════════════════");
            System.out.println("  运行算法: " + algorithm);
            System.out.println("════════════════════════════════════════════════════════════════");
            
            // 为每个算法配置能耗策略
            // NSGA-II启用开关机策略，其他算法关闭开关机策略
            boolean enableSwitchingStrategy = algorithm.equals("NSGAII") ||  algorithm.equals("MOGWO");
            
            if (enableSwitchingStrategy) {
                System.out.println("  ⚡ 能耗策略: 智能开关机策略（已启用）");
            } else {
                System.out.println("  ⚡ 能耗策略: 纯待机策略（开关机已关闭）");
            }
            
            AlgorithmResults results = runAlgorithm(
                algorithm, problem, enableSwitchingStrategy, instanceName, 
                instanceOutputDir, REPEAT_TIMES
            );
            
            allResults.put(algorithm, results.paretoFronts);
            allRunTimes.put(algorithm, results.runTimes);
            
            // 输出每次运行的结果到文件
            saveAlgorithmResults(algorithm, instanceName, instanceOutputDir, results);
        }
        
        // 计算近似Pareto前沿（PF*）
        System.out.println("\n════════════════════════════════════════════════════════════════");
        System.out.println("  计算近似Pareto前沿 (PF*)");
        System.out.println("════════════════════════════════════════════════════════════════");
        
        List<MOIndividual> approximateParetoFront = computeApproximateParetoFront(allResults);
        System.out.println("✅ PF*计算完成，包含 " + approximateParetoFront.size() + " 个非支配解");
        
        // 保存PF*
        saveParetoFront(approximateParetoFront, instanceOutputDir + "ApproximateParetoFront.txt");
        
        // 计算归一化范围
        System.out.println("\n════════════════════════════════════════════════════════════════");
        System.out.println("  归一化处理");
        System.out.println("════════════════════════════════════════════════════════════════");
        
        NormalizationInfo normInfo = calculateNormalizationRange(allResults);
        System.out.println("✅ 归一化范围确定完成");
        System.out.println("   Cmax范围:   [" + String.format("%.2f", normInfo.minValues[0]) + 
                         ", " + String.format("%.2f", normInfo.maxValues[0]) + "]");
        System.out.println("   Energy范围: [" + String.format("%.2f", normInfo.minValues[1]) + 
                         ", " + String.format("%.2f", normInfo.maxValues[1]) + "]");
        
        // 计算性能指标
        System.out.println("\n════════════════════════════════════════════════════════════════");
        System.out.println("  计算性能指标");
        System.out.println("════════════════════════════════════════════════════════════════");
        
        Map<String, PerformanceMetrics> metricsMap = new HashMap<>();
        
        for (String algorithm : ALGORITHMS) {
            System.out.println("\n【" + algorithm + "】");
            
            List<List<MOIndividual>> algResults = allResults.get(algorithm);
            PerformanceMetrics metrics = calculateMetrics(
                algResults, approximateParetoFront, normInfo
            );
            
            metricsMap.put(algorithm, metrics);
            
            System.out.println("  HV:  " + String.format("%.6f ± %.6f", metrics.avgHV, metrics.stdHV));
            System.out.println("  IGD: " + String.format("%.6f ± %.6f", metrics.avgIGD, metrics.stdIGD));
        }
        
        // 计算C-metric（两两对比）
        System.out.println("\n【C-metric (覆盖率)】");
        Map<String, Map<String, Double>> cMetricMap = calculateCMetric(allResults);
        
        for (String algA : ALGORITHMS) {
            for (String algB : ALGORITHMS) {
                if (!algA.equals(algB)) {
                    double cValue = cMetricMap.get(algA).get(algB);
                    System.out.println(String.format("  C(%s, %s) = %.4f", algA, algB, cValue));
                }
            }
        }
        
        // 生成对比报告
        System.out.println("\n════════════════════════════════════════════════════════════════");
        System.out.println("  生成对比报告");
        System.out.println("════════════════════════════════════════════════════════════════");
        
        generateComparisonReport(
            instanceName, instanceOutputDir, metricsMap, 
            cMetricMap, allRunTimes, normInfo, approximateParetoFront.size()
        );
        
        // 生成指标数据文件（用于画箱线图）
        System.out.println("\n════════════════════════════════════════════════════════════════");
        System.out.println("  生成指标数据文件");
        System.out.println("════════════════════════════════════════════════════════════════");
        
        generateMetricsDataFile(
            instanceName, instanceOutputDir, metricsMap, allRunTimes
        );
        
        System.out.println("\n✅ 算例 " + instanceName + " 的对比实验完成！");
        System.out.println("   结果保存在: " + instanceOutputDir);
    }
    
    /**
     * 运行单个算法
     */
    private static AlgorithmResults runAlgorithm(
            String algorithm,
            Problem problem,
            boolean enableSwitchingStrategy,
            String instanceName,
            String outputDir,
            int repeatTimes) {
        
        List<List<MOIndividual>> paretoFronts = new ArrayList<>();
        List<Double> runTimes = new ArrayList<>();
        
        if (ENABLE_MULTI_THREAD) {
            // 多线程模式
            int threadCount = THREAD_POOL_SIZE > 0 ? THREAD_POOL_SIZE : 
                             Runtime.getRuntime().availableProcessors();
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            
            List<Future<AlgorithmTask.Result>> futures = new ArrayList<>();
            
            // 为每个线程创建独立任务（每个任务内部创建独立的objectives）
            for (int run = 1; run <= repeatTimes; run++) {
                AlgorithmTask task = new AlgorithmTask(
                    algorithm, problem, enableSwitchingStrategy, run
                );
                futures.add(executor.submit(task));
            }
            
            try {
                for (int i = 0; i < futures.size(); i++) {
                    AlgorithmTask.Result result = futures.get(i).get();
                    paretoFronts.add(result.paretoFront);
                    runTimes.add(result.runTime);
                    
                    System.out.println(String.format("  运行 %d/%d 完成 (Pareto规模=%d, 耗时=%.1f秒)",
                        i + 1, repeatTimes, result.paretoFront.size(), result.runTime));
                }
            } catch (Exception e) {
                System.err.println("❌ 多线程执行出错: " + e.getMessage());
                e.printStackTrace();
            } finally {
                executor.shutdown();
            }
            
        } else {
            // 单线程模式
            for (int run = 1; run <= repeatTimes; run++) {
                System.out.println("  运行 " + run + "/" + repeatTimes);
                
                // 为每次运行创建独立的objectives
                List<MOEvaluator.ObjectiveFunction> objectives = 
                    configureObjectives(problem, enableSwitchingStrategy);
                
                long startTime = System.currentTimeMillis();
                List<MOIndividual> paretoFront = runSingleAlgorithm(
                    algorithm, problem, objectives, run
                );
                long endTime = System.currentTimeMillis();
                
                double runTime = (endTime - startTime) / 1000.0;
                
                paretoFronts.add(paretoFront);
                runTimes.add(runTime);
                
                System.out.println("    ✅ Pareto规模=" + paretoFront.size() + 
                                 ", 耗时=" + String.format("%.1f", runTime) + "秒");
            }
        }
        
        // 初始化工序矩阵（用于生成图表）
        int jobCount = problem.getJobCount();
        int[] opsCount = problem.getOperationCountArr();
        ProgramEntity.Operation[][] operationMatrix = new ProgramEntity.Operation[jobCount][];
        for (int i = 0; i < jobCount; i++) {
            operationMatrix[i] = new ProgramEntity.Operation[opsCount[i]];
            for (int j = 0; j < opsCount[i]; j++) {
                operationMatrix[i][j] = new ProgramEntity.Operation();
            }
        }
        
        AlgorithmResults results = new AlgorithmResults();
        results.paretoFronts = paretoFronts;
        results.runTimes = runTimes;
        results.problem = problem;
        results.operationMatrix = operationMatrix;
        
        return results;
    }
    
    /**
     * 运行单次算法
     */
    private static List<MOIndividual> runSingleAlgorithm(
            String algorithm,
            Problem problem,
            List<MOEvaluator.ObjectiveFunction> objectives,
            int runNo) {
        
        long seed = new Random().nextLong();
        
        switch (algorithm) {
            case "NSGAII":
                return runNSGAII(problem, objectives, seed);
            case "NSGAII-Basic":
                return runNSGAIIBasic(problem, objectives, seed);
            case "MOEAD":
                return runMOEAD(problem, objectives, seed);
            case "MOGWO":
                return runMOGWO(problem, objectives, seed);
            case "SPEA2":
                return runSPEA2(problem, objectives, seed);
            default:
                throw new IllegalArgumentException("未知算法: " + algorithm);
        }
    }
    
    /**
     * 运行NSGA-II（改进版：启发式初始化 + 局部搜索）
     */
    private static List<MOIndividual> runNSGAII(
            Problem problem,
            List<MOEvaluator.ObjectiveFunction> objectives,
            long seed) {
        
        NSGAII nsgaii = new NSGAII(problem, objectives);
        nsgaii.setPopulationSize(POPULATION_SIZE);
        nsgaii.setUseStrategy(true);
        nsgaii.setCrossoverRate(CROSSOVER_RATE);
        nsgaii.setMutationRate(MUTATION_RATE);
        nsgaii.setMaxGenerations(MAX_GENERATIONS);
        nsgaii.setMaxRunTimeMinutes(MAX_RUN_TIME);
        nsgaii.setSeed(seed);
        nsgaii.enableLocalSearch(true, 10);
        nsgaii.setLocalSearchParameters(
                LOCAL_SEARCH_L, 0.5, 0.02, 0.0002, 0, 0
        );
        
        return nsgaii.solve();
    }
    
    /**
     * 运行NSGA-II-Basic（基础版：随机初始化 + 无局部搜索）
     * 
     * 说明：
     * - 使用完全随机的初始化方式
     * - 不启用局部搜索
     * - 作为基准算法，用于对比改进效果
     */
    private static List<MOIndividual> runNSGAIIBasic(
            Problem problem,
            List<MOEvaluator.ObjectiveFunction> objectives,
            long seed) {
        
        NSGAII nsgaii = new NSGAII(problem, objectives);
        nsgaii.setPopulationSize(POPULATION_SIZE);
        nsgaii.setCrossoverRate(CROSSOVER_RATE);
        nsgaii.setMutationRate(MUTATION_RATE);
        nsgaii.setMaxGenerations(MAX_GENERATIONS);
        nsgaii.setMaxRunTimeMinutes(MAX_RUN_TIME);
        nsgaii.setSeed(seed);
        
        // 基础版：不启用局部搜索
        // 注意：当前NSGAII的初始化已经包含了混合策略（部分随机）
        // 这里通过不启用局部搜索来体现基础版本的特点
        
        return nsgaii.solve();
    }
    
    /**
     * 运行MOEA/D（基础版：随机初始化 + 无局部搜索）
     */
    private static List<MOIndividual> runMOEAD(
            Problem problem,
            List<MOEvaluator.ObjectiveFunction> objectives,
            long seed) {
        
        MOEAD moead = new MOEAD(problem, objectives);
        moead.setPopulationSize(POPULATION_SIZE);
        moead.setCrossoverRate(CROSSOVER_RATE);
        moead.setMutationRate(MUTATION_RATE);
        moead.setMaxGenerations(MAX_GENERATIONS);
        moead.setMaxRunTimeMinutes(MAX_RUN_TIME);
        moead.setSeed(seed);
        moead.enableLocalSearch(true, 1);
        moead.setLocalSearchParameters(
                LOCAL_SEARCH_L, 1.0, 0, 0, 0, 0
        );
        // 不启用局部搜索（作为基准对比）
        // 注意：MOEA/D的初始化默认使用混合策略
        
        return moead.solve();
    }
    
    /**
     * 运行MOGWO（基础版：随机初始化 + 无局部搜索）
     */
    private static List<MOIndividual> runMOGWO(
            Problem problem,
            List<MOEvaluator.ObjectiveFunction> objectives,
            long seed) {
        
        MOGWO mogwo = new MOGWO(problem, objectives);
        mogwo.setPopulationSize(POPULATION_SIZE);
        mogwo.setArchiveSize(POPULATION_SIZE);  // 存档大小=种群大小
        mogwo.setMaxGenerations(MAX_GENERATIONS);
        mogwo.setMaxRunTimeMinutes(MAX_RUN_TIME);
        mogwo.setSeed(seed);
        
        // 不启用局部搜索（作为基准对比）
        // 注意：MOGWO的初始化默认使用混合策略
        mogwo.enableLocalSearch(true, 1);
        mogwo.setLocalSearchParameters(
                LOCAL_SEARCH_L, 1.0, 0, 0, 0, 0
        );
        return mogwo.solve();
    }
    
    /**
     * 运行SPEA2（基础版：随机初始化 + 无局部搜索）
     */
    private static List<MOIndividual> runSPEA2(
            Problem problem,
            List<MOEvaluator.ObjectiveFunction> objectives,
            long seed) {
        
        SPEA2 spea2 = new SPEA2(problem, objectives);
        spea2.setPopulationSize(POPULATION_SIZE);
        spea2.setArchiveSize(POPULATION_SIZE);  // 存档大小=种群大小
        spea2.setCrossoverRate(CROSSOVER_RATE);
        spea2.setMutationRate(MUTATION_RATE);
        spea2.setMaxGenerations(MAX_GENERATIONS);
        spea2.setMaxRunTimeMinutes(MAX_RUN_TIME);
        spea2.setSeed(seed);
        spea2.enableLocalSearch(true, 1);
        // 不启用局部搜索（作为基准对比）
        // 注意：SPEA2的初始化默认使用混合策略
        
        return spea2.solve();
    }
    
    /**
     * 保存算法结果（每次运行）- 完整输出版本
     */
    private static void saveAlgorithmResults(
            String algorithm,
            String instanceName,
            String baseOutputDir,
            AlgorithmResults results) throws IOException {
        
        // 创建算法目录
        String algorithmDir = baseOutputDir + algorithm + "/";
        new File(algorithmDir).mkdirs();
        
        // 保存每次运行的完整结果
        for (int i = 0; i < results.paretoFronts.size(); i++) {
            String runDir = algorithmDir + "run_" + (i + 1) + "/";
            new File(runDir).mkdirs();
            
            List<MOIndividual> paretoFront = results.paretoFronts.get(i);
            
            // 去重：移除目标值相同的解
            List<MOIndividual> uniqueParetoFront = removeDuplicates(paretoFront);
            
            System.out.println("\n  正在保存 " + algorithm + " 运行" + (i+1) + " 的结果...");
            
            // 1. 保存Pareto前沿数据文件
            String paretoFile = runDir + "ParetoFront.txt";
            saveParetoFront(uniqueParetoFront, paretoFile);
            
            // 2. 生成Pareto前沿图
            try {
                String paretoChartFile = runDir + "pareto_front.png";
                AlgorithmFrame.nsgaii.ParetoFrontVisualizer.generateParetoFrontChart(
                    uniqueParetoFront, paretoChartFile, 1200, 800
                );
                System.out.println("    ✓ Pareto前沿图已保存");
            } catch (Exception e) {
                System.out.println("    ⚠ Pareto前沿图生成失败: " + e.getMessage());
            }
            
            // 3. 保存Pareto前沿详细数据
            String paretoDataFile = runDir + "pareto_front_data.txt";
            exportParetoFrontData(uniqueParetoFront, paretoDataFile, instanceName, algorithm, i+1);
            
            // 4. 找到最优Cmax解
            MOIndividual bestCmaxSolution = findBestCmaxSolution(uniqueParetoFront);
            
            // 5. 生成最优解的甘特图
            try {
                String ganttFile = runDir + "best_cmax_gantt.png";
                exportGanttChart(bestCmaxSolution, ganttFile, results.problem, results.operationMatrix);
                System.out.println("    ✓ 甘特图已保存");
            } catch (Exception e) {
                System.out.println("    ⚠ 甘特图生成失败: " + e.getMessage());
            }
            
            // 6. 生成打印批次排布图
            try {
                String layoutFile = runDir + "best_cmax_printer_layout.png";
                exportPrinterLayout(bestCmaxSolution, layoutFile, results.problem);
                System.out.println("    ✓ 打印批次排布图已保存");
            } catch (Exception e) {
                System.out.println("    ⚠ 打印批次排布图生成失败: " + e.getMessage());
            }
            
            // 7. 保存最优解的详细调度记录
            try {
                String scheduleFile = runDir + "best_cmax_schedule_records.txt";
                exportScheduleRecords(bestCmaxSolution, scheduleFile, results.problem, results.operationMatrix);
                System.out.println("    ✓ 详细调度记录已保存");
            } catch (Exception e) {
                System.out.println("    ⚠ 详细调度记录保存失败: " + e.getMessage());
            }
            
            // 8. 保存统计信息摘要
            String statsFile = runDir + "Statistics.txt";
            try (PrintWriter writer = new PrintWriter(new FileWriter(statsFile))) {
                writer.println("════════════════════════════════════════════════════════════════");
                writer.println("              运行统计信息");
                writer.println("════════════════════════════════════════════════════════════════");
                writer.println();
                writer.println("算例: " + instanceName);
                writer.println("算法: " + algorithm);
                writer.println("运行次数: " + (i + 1));
                writer.println("Pareto前沿规模（去重前）: " + paretoFront.size());
                writer.println("Pareto前沿规模（去重后）: " + uniqueParetoFront.size());
                writer.println("运行时间: " + String.format("%.2f", results.runTimes.get(i)) + " 秒");
                writer.println();
                writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                writer.println("  Pareto前沿（已去重）");
                writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                writer.println();
                writer.println(String.format("%-8s %-15s %-15s %-15s %-10s", 
                    "序号", "Cmax", "能耗(kWh)", "装箱质量", "批次数"));
                writer.println(repeatString("─", 70));
                for (int j = 0; j < uniqueParetoFront.size(); j++) {
                    MOIndividual ind = uniqueParetoFront.get(j);
                    writer.println(String.format("%-8d %-15.2f %-15.2f %-15.4f %-10d", 
                        j + 1, ind.objectives[0], ind.objectives[1], 
                        ind.packingQ, ind.batchCount));
                }
                writer.println();
                writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                writer.println("  最优Cmax解");
                writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                writer.println();
                writer.println("Cmax: " + String.format("%.2f", bestCmaxSolution.objectives[0]));
                writer.println("能耗: " + String.format("%.2f", bestCmaxSolution.objectives[1]) + " kWh");
                writer.println("装箱质量: " + String.format("%.4f", bestCmaxSolution.packingQ));
                writer.println("批次数: " + bestCmaxSolution.batchCount);
                writer.println();
                writer.println("详细信息请查看:");
                writer.println("  - pareto_front.png (Pareto前沿图)");
                writer.println("  - pareto_front_data.txt (Pareto前沿详细数据)");
                writer.println("  - best_cmax_gantt.png (甘特图)");
                writer.println("  - best_cmax_printer_layout.png (打印批次排布图)");
                writer.println("  - best_cmax_schedule_records.txt (详细调度记录)");
                writer.println();
                writer.println("════════════════════════════════════════════════════════════════");
            }
        }
        
        // 汇总该算法的所有非支配解
        List<MOIndividual> allSolutions = new ArrayList<>();
        for (List<MOIndividual> front : results.paretoFronts) {
            allSolutions.addAll(front);
        }
        
        // 提取该算法的总体Pareto前沿
        List<MOIndividual> algorithmParetoFront = extractNonDominatedSolutions(allSolutions);
        
        // 保存算法总体Pareto前沿
        String algorithmParetoFile = algorithmDir + "AlgorithmParetoFront.txt";
        saveParetoFront(algorithmParetoFront, algorithmParetoFile);
        
        System.out.println("  ✅ " + algorithm + " 结果已保存");
        System.out.println("     总体Pareto前沿: " + algorithmParetoFront.size() + " 个解");
    }
    
    /**
     * 计算近似Pareto前沿（所有算法所有运行的非支配解合集）
     */
    private static List<MOIndividual> computeApproximateParetoFront(
            Map<String, List<List<MOIndividual>>> allResults) {
        
        List<MOIndividual> allSolutions = new ArrayList<>();
        
        // 收集所有解
        for (List<List<MOIndividual>> algResults : allResults.values()) {
            for (List<MOIndividual> front : algResults) {
                allSolutions.addAll(front);
            }
        }
        
        System.out.println("总解数: " + allSolutions.size());
        
        // 提取非支配解
        List<MOIndividual> paretoFront = extractNonDominatedSolutions(allSolutions);
        
        return paretoFront;
    }
    
    /**
     * 提取非支配解
     */
    private static List<MOIndividual> extractNonDominatedSolutions(
            List<MOIndividual> solutions) {
        
        List<MOIndividual> nonDominated = new ArrayList<>();
        
        for (MOIndividual candidate : solutions) {
            boolean isDominated = false;
            
            for (MOIndividual other : solutions) {
                if (candidate == other) continue;
                
                if (dominates(other, candidate)) {
                    isDominated = true;
                    break;
                }
            }
            
            if (!isDominated) {
                // 检查是否已存在相同的解
                boolean exists = false;
                for (MOIndividual existing : nonDominated) {
                    if (isSameObjectives(existing, candidate)) {
                        exists = true;
                        break;
                    }
                }
                
                if (!exists) {
                    nonDominated.add(candidate);
                }
            }
        }
        
        return nonDominated;
    }
    
    /**
     * 判断a是否支配b
     */
    private static boolean dominates(MOIndividual a, MOIndividual b) {
        boolean atLeastOneBetter = false;
        
        for (int i = 0; i < a.objectives.length; i++) {
            if (a.objectives[i] > b.objectives[i]) {
                return false;  // a在某个目标上更差
            }
            if (a.objectives[i] < b.objectives[i]) {
                atLeastOneBetter = true;  // a在某个目标上更好
            }
        }
        
        return atLeastOneBetter;
    }
    
    /**
     * 判断两个解的目标值是否相同
     */
    private static boolean isSameObjectives(MOIndividual a, MOIndividual b) {
        if (a.objectives.length != b.objectives.length) {
            return false;
        }
        
        for (int i = 0; i < a.objectives.length; i++) {
            if (Math.abs(a.objectives[i] - b.objectives[i]) > 1e-6) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 移除目标值重复的解
     * 
     * @param solutions 原始解集
     * @return 去重后的解集
     */
    private static List<MOIndividual> removeDuplicates(List<MOIndividual> solutions) {
        List<MOIndividual> uniqueSolutions = new ArrayList<>();
        
        for (MOIndividual candidate : solutions) {
            boolean isDuplicate = false;
            
            // 检查是否已存在相同目标值的解
            for (MOIndividual existing : uniqueSolutions) {
                if (isSameObjectives(candidate, existing)) {
                    isDuplicate = true;
                    break;
                }
            }
            
            // 如果不重复，添加到结果中
            if (!isDuplicate) {
                uniqueSolutions.add(candidate);
            }
        }
        
        return uniqueSolutions;
    }
    
    /**
     * 计算归一化范围
     */
    private static NormalizationInfo calculateNormalizationRange(
            Map<String, List<List<MOIndividual>>> allResults) {
        
        double[] minValues = new double[2];
        double[] maxValues = new double[2];
        Arrays.fill(minValues, Double.POSITIVE_INFINITY);
        Arrays.fill(maxValues, Double.NEGATIVE_INFINITY);
        
        for (List<List<MOIndividual>> algResults : allResults.values()) {
            for (List<MOIndividual> front : algResults) {
                for (MOIndividual ind : front) {
                    minValues[0] = Math.min(minValues[0], ind.objectives[0]);
                    minValues[1] = Math.min(minValues[1], ind.objectives[1]);
                    maxValues[0] = Math.max(maxValues[0], ind.objectives[0]);
                    maxValues[1] = Math.max(maxValues[1], ind.objectives[1]);
                }
            }
        }
        
        return new NormalizationInfo(minValues, maxValues);
    }
    
    /**
     * 归一化Pareto前沿
     */
    private static List<MOIndividual> normalizeParetoFront(
            List<MOIndividual> front,
            double[] minValues,
            double[] maxValues) {
        
        List<MOIndividual> normalizedFront = new ArrayList<>();
        
        for (MOIndividual ind : front) {
            MOIndividual normalized = new MOIndividual(ind);
            
            for (int i = 0; i < ind.objectives.length; i++) {
                double range = maxValues[i] - minValues[i];
                if (range > 0) {
                    normalized.objectives[i] = (ind.objectives[i] - minValues[i]) / range;
                } else {
                    normalized.objectives[i] = 0.0;
                }
            }
            
            normalizedFront.add(normalized);
        }
        
        return normalizedFront;
    }
    
    /**
     * 计算性能指标
     */
    private static PerformanceMetrics calculateMetrics(
            List<List<MOIndividual>> algResults,
            List<MOIndividual> approximateParetoFront,
            NormalizationInfo normInfo) {
        
        List<Double> hvValues = new ArrayList<>();
        List<Double> igdValues = new ArrayList<>();
        
        // 归一化参考点（用于HV计算）
        double[] normalizedReferencePoint = {1.1, 1.1};
        
        // 归一化近似Pareto前沿（用于IGD计算）
        List<MOIndividual> normalizedPF = normalizeParetoFront(
            approximateParetoFront, normInfo.minValues, normInfo.maxValues
        );
        
        for (List<MOIndividual> front : algResults) {
            // 归一化当前前沿
            List<MOIndividual> normalizedFront = normalizeParetoFront(
                front, normInfo.minValues, normInfo.maxValues
            );
            
            // 计算HV
            double hv = calculateHypervolume2D(normalizedFront, normalizedReferencePoint);
            hvValues.add(hv);
            
            // 计算IGD
            double igd = calculateIGD(normalizedFront, normalizedPF);
            igdValues.add(igd);
        }
        
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.avgHV = calculateMean(hvValues);
        metrics.stdHV = calculateStdDev(hvValues, metrics.avgHV);
        metrics.minHV = Collections.min(hvValues);
        metrics.maxHV = Collections.max(hvValues);
        
        metrics.avgIGD = calculateMean(igdValues);
        metrics.stdIGD = calculateStdDev(igdValues, metrics.avgIGD);
        metrics.minIGD = Collections.min(igdValues);
        metrics.maxIGD = Collections.max(igdValues);
        
        metrics.hvValues = hvValues;
        metrics.igdValues = igdValues;
        
        return metrics;
    }
    
    /**
     * 计算2D超体积（Hypervolume）
     */
    private static double calculateHypervolume2D(
            List<MOIndividual> paretoFront,
            double[] referencePoint) {
        
        if (paretoFront.isEmpty()) {
            return 0.0;
        }
        
        // 按第一个目标排序
        List<MOIndividual> sorted = new ArrayList<>(paretoFront);
        sorted.sort(Comparator.comparingDouble(ind -> ind.objectives[0]));
        
        double hypervolume = 0.0;
        double prevY = referencePoint[1];
        
        for (MOIndividual ind : sorted) {
            double x = ind.objectives[0];
            double y = ind.objectives[1];
            
            if (x < referencePoint[0] && y < referencePoint[1]) {
                hypervolume += (referencePoint[0] - x) * (prevY - y);
                prevY = y;
            }
        }
        
        return hypervolume;
    }
    
    /**
     * 计算IGD（Inverted Generational Distance）
     */
    private static double calculateIGD(
            List<MOIndividual> obtainedFront,
            List<MOIndividual> referenceFront) {
        
        if (referenceFront.isEmpty() || obtainedFront.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        
        double sumDistance = 0.0;
        
        // 对参考前沿中的每个点，找到获得前沿中最近的点
        for (MOIndividual refPoint : referenceFront) {
            double minDistance = Double.POSITIVE_INFINITY;
            
            for (MOIndividual obtainedPoint : obtainedFront) {
                double distance = euclideanDistance(refPoint, obtainedPoint);
                minDistance = Math.min(minDistance, distance);
            }
            
            sumDistance += minDistance;
        }
        
        return sumDistance / referenceFront.size();
    }
    
    /**
     * 计算欧氏距离
     */
    private static double euclideanDistance(MOIndividual a, MOIndividual b) {
        double sum = 0.0;
        for (int i = 0; i < a.objectives.length; i++) {
            double diff = a.objectives[i] - b.objectives[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
    
    /**
     * 计算C-metric（覆盖率）
     */
    private static Map<String, Map<String, Double>> calculateCMetric(
            Map<String, List<List<MOIndividual>>> allResults) {
        
        Map<String, Map<String, Double>> cMetricMap = new HashMap<>();
        
        // 先汇总每个算法的所有解
        Map<String, List<MOIndividual>> algorithmFronts = new HashMap<>();
        
        for (String algorithm : allResults.keySet()) {
            List<MOIndividual> allSolutions = new ArrayList<>();
            for (List<MOIndividual> front : allResults.get(algorithm)) {
                allSolutions.addAll(front);
            }
            // 提取非支配解
            algorithmFronts.put(algorithm, extractNonDominatedSolutions(allSolutions));
        }
        
        // 计算两两之间的C-metric
        for (String algA : ALGORITHMS) {
            Map<String, Double> row = new HashMap<>();
            
            for (String algB : ALGORITHMS) {
                if (algA.equals(algB)) {
                    row.put(algB, 0.0);
                } else {
                    double cValue = computeCMetric(
                        algorithmFronts.get(algA),
                        algorithmFronts.get(algB)
                    );
                    row.put(algB, cValue);
                }
            }
            
            cMetricMap.put(algA, row);
        }
        
        return cMetricMap;
    }
    
    /**
     * 计算C(A, B)：A支配B的比例
     */
    private static double computeCMetric(List<MOIndividual> frontA, List<MOIndividual> frontB) {
        if (frontB.isEmpty()) {
            return 0.0;
        }
        
        int dominatedCount = 0;
        
        for (MOIndividual b : frontB) {
            for (MOIndividual a : frontA) {
                if (dominates(a, b)) {
                    dominatedCount++;
                    break;  // b被a支配，不需要检查其他a
                }
            }
        }
        
        return (double) dominatedCount / frontB.size();
    }
    
    /**
     * 生成对比报告
     */
    private static void generateComparisonReport(
            String instanceName,
            String outputDir,
            Map<String, PerformanceMetrics> metricsMap,
            Map<String, Map<String, Double>> cMetricMap,
            Map<String, List<Double>> allRunTimes,
            NormalizationInfo normInfo,
            int pfStarSize) throws IOException {
        
        String reportFile = outputDir + "ComparisonReport.txt";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
            // 标题
            writer.println("════════════════════════════════════════════════════════════════");
            writer.println("              算法对比实验报告");
            writer.println("════════════════════════════════════════════════════════════════");
            writer.println();
            writer.println("生成时间: " + new Date());
            writer.println("算例名称: " + instanceName);
            writer.println("对比算法: " + String.join(", ", ALGORITHMS));
            writer.println("重复次数: " + REPEAT_TIMES + " 次");
            writer.println("近似Pareto前沿(PF*)规模: " + pfStarSize);
            writer.println();
            
            // 归一化范围
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  归一化范围");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println();
            writer.println("Cmax:   [" + String.format("%.2f", normInfo.minValues[0]) + 
                         ", " + String.format("%.2f", normInfo.maxValues[0]) + "]");
            writer.println("Energy: [" + String.format("%.2f", normInfo.minValues[1]) + 
                         ", " + String.format("%.2f", normInfo.maxValues[1]) + "]");
            writer.println();
            
            // Hypervolume结果
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  Hypervolume (HV) 结果");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println();
            writer.println(String.format("%-12s %-15s %-15s %-15s %-15s",
                "算法", "平均HV", "标准差", "最大HV", "最小HV"));
            writer.println(repeatString("─", 75));
            
            for (String algorithm : ALGORITHMS) {
                PerformanceMetrics metrics = metricsMap.get(algorithm);
                writer.println(String.format("%-12s %-15.6f %-15.6f %-15.6f %-15.6f",
                    algorithm, metrics.avgHV, metrics.stdHV, metrics.maxHV, metrics.minHV));
            }
            writer.println();
            
            // IGD结果
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  IGD (Inverted Generational Distance) 结果");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println();
            writer.println(String.format("%-12s %-15s %-15s %-15s %-15s",
                "算法", "平均IGD", "标准差", "最大IGD", "最小IGD"));
            writer.println(repeatString("─", 75));
            
            for (String algorithm : ALGORITHMS) {
                PerformanceMetrics metrics = metricsMap.get(algorithm);
                writer.println(String.format("%-12s %-15.6f %-15.6f %-15.6f %-15.6f",
                    algorithm, metrics.avgIGD, metrics.stdIGD, metrics.maxIGD, metrics.minIGD));
            }
            writer.println();
            
            // C-metric结果
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  C-metric (覆盖率) 结果");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println();
            writer.println("C(A, B) 表示：A的解集中有多少比例支配B的解集");
            writer.println();
            
            // C-metric矩阵
            writer.print(String.format("%-12s", ""));
            for (String algB : ALGORITHMS) {
                writer.print(String.format("%-12s", algB));
            }
            writer.println();
            writer.println(repeatString("─", 12 * (ALGORITHMS.length + 1)));
            
            for (String algA : ALGORITHMS) {
                writer.print(String.format("%-12s", algA));
                for (String algB : ALGORITHMS) {
                    double cValue = cMetricMap.get(algA).get(algB);
                    writer.print(String.format("%-12.4f", cValue));
                }
                writer.println();
            }
            writer.println();
            
            // 运行时间统计
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  运行时间统计 (秒)");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println();
            writer.println(String.format("%-12s %-15s %-15s %-15s %-15s",
                "算法", "平均时间", "标准差", "最大时间", "最小时间"));
            writer.println(repeatString("─", 75));
            
            for (String algorithm : ALGORITHMS) {
                List<Double> runTimes = allRunTimes.get(algorithm);
                double avgTime = calculateMean(runTimes);
                double stdTime = calculateStdDev(runTimes, avgTime);
                double minTime = Collections.min(runTimes);
                double maxTime = Collections.max(runTimes);
                
                writer.println(String.format("%-12s %-15.2f %-15.2f %-15.2f %-15.2f",
                    algorithm, avgTime, stdTime, maxTime, minTime));
            }
            writer.println();
            
            // 详细数据
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  详细数据");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println();
            
            for (String algorithm : ALGORITHMS) {
                writer.println("【" + algorithm + "】");
                PerformanceMetrics metrics = metricsMap.get(algorithm);
                
                writer.println("HV值:");
                for (int i = 0; i < metrics.hvValues.size(); i++) {
                    writer.print(String.format("  运行%2d: %.6f", i + 1, metrics.hvValues.get(i)));
                    if ((i + 1) % 5 == 0) writer.println();
                }
                writer.println();
                
                writer.println("IGD值:");
                for (int i = 0; i < metrics.igdValues.size(); i++) {
                    writer.print(String.format("  运行%2d: %.6f", i + 1, metrics.igdValues.get(i)));
                    if ((i + 1) % 5 == 0) writer.println();
                }
                writer.println("\n");
            }
            
            // 结束
            writer.println("════════════════════════════════════════════════════════════════");
            writer.println("                      报告结束");
            writer.println("════════════════════════════════════════════════════════════════");
        }
        
        System.out.println("✅ 对比报告已保存到: " + reportFile);
    }
    
    /**
     * 保存Pareto前沿
     */
    private static void saveParetoFront(List<MOIndividual> paretoFront, String filePath) 
            throws IOException {
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("# Pareto Front");
            writer.println("# Size: " + paretoFront.size());
            writer.println("# Format: Cmax Energy");
            writer.println();
            
            for (MOIndividual ind : paretoFront) {
                writer.println(String.format("%.6f\t%.6f", 
                    ind.objectives[0], ind.objectives[1]));
            }
        }
    }
    
    /**
     * 生成指标数据文件（用于画箱线图）
     * 
     * @param instanceName 算例名称
     * @param outputDir 输出目录
     * @param metricsMap 性能指标映射
     * @param allRunTimes 运行时间映射
     */
    private static void generateMetricsDataFile(
            String instanceName,
            String outputDir,
            Map<String, PerformanceMetrics> metricsMap,
            Map<String, List<Double>> allRunTimes) throws IOException {
        
        // 生成CSV格式的指标数据文件
        String csvFile = outputDir + "MetricsData.csv";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            // 写入CSV头部
            writer.println("Algorithm,Run,HV,IGD,RunTime");
            
            // 写入每个算法每次运行的数据
            for (String algorithm : ALGORITHMS) {
                PerformanceMetrics metrics = metricsMap.get(algorithm);
                List<Double> runTimes = allRunTimes.get(algorithm);
                
                // 确保数据存在
                if (metrics != null && metrics.hvValues != null && metrics.igdValues != null) {
                    for (int i = 0; i < metrics.hvValues.size(); i++) {
                        double hv = metrics.hvValues.get(i);
                        double igd = metrics.igdValues.get(i);
                        double runTime = runTimes.get(i);
                        
                        writer.println(String.format("%s,%d,%.8f,%.8f,%.2f",
                            algorithm, i + 1, hv, igd, runTime));
                    }
                }
            }
        }
        
        System.out.println("✅ 指标数据文件已保存: " + csvFile);
        System.out.println("   格式: CSV (用于画箱线图)");
        
        // 额外生成TXT格式的详细数据文件（更易读）
        String txtFile = outputDir + "MetricsData.txt";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(txtFile))) {
            writer.println("════════════════════════════════════════════════════════════════");
            writer.println("              指标数据文件（用于画箱线图）");
            writer.println("════════════════════════════════════════════════════════════════");
            writer.println();
            writer.println("算例名称: " + instanceName);
            writer.println("生成时间: " + new Date());
            writer.println("对比算法: " + String.join(", ", ALGORITHMS));
            writer.println("重复次数: " + REPEAT_TIMES);
            writer.println();
            writer.println("说明: 本文件包含所有算法每次运行的详细指标数据");
            writer.println("      可用于生成箱线图、统计分析等");
            writer.println();
            
            // 为每个算法生成详细数据
            for (String algorithm : ALGORITHMS) {
                writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                writer.println("  " + algorithm);
                writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                writer.println();
                
                PerformanceMetrics metrics = metricsMap.get(algorithm);
                List<Double> runTimes = allRunTimes.get(algorithm);
                
                if (metrics != null && metrics.hvValues != null && metrics.igdValues != null) {
                    // 表头
                    writer.println(String.format("%-8s %-15s %-15s %-12s",
                        "运行次数", "HV", "IGD", "运行时间(秒)"));
                    writer.println(repeatString("─", 55));
                    
                    // 数据行
                    for (int i = 0; i < metrics.hvValues.size(); i++) {
                        writer.println(String.format("%-8d %-15.8f %-15.8f %-12.2f",
                            i + 1,
                            metrics.hvValues.get(i),
                            metrics.igdValues.get(i),
                            runTimes.get(i)));
                    }
                    
                    writer.println();
                    
                    // 统计信息
                    writer.println("统计信息:");
                    writer.println("  HV  - 平均值: " + String.format("%.8f", metrics.avgHV) + 
                                 ", 标准差: " + String.format("%.8f", metrics.stdHV));
                    writer.println("  IGD - 平均值: " + String.format("%.8f", metrics.avgIGD) + 
                                 ", 标准差: " + String.format("%.8f", metrics.stdIGD));
                    
                    double avgTime = runTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    double stdTime = calculateStdDev(runTimes, avgTime);
                    writer.println("  时间 - 平均值: " + String.format("%.2f", avgTime) + "秒" +
                                 ", 标准差: " + String.format("%.2f", stdTime) + "秒");
                    writer.println();
                }
            }
            
            writer.println("════════════════════════════════════════════════════════════════");
            writer.println("                      文件结束");
            writer.println("════════════════════════════════════════════════════════════════");
        }
        
        System.out.println("✅ 指标详细数据已保存: " + txtFile);
        System.out.println("   格式: TXT (易读格式)");
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 选择算例
     */
    private static String selectInstance() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("请选择算例:");
        System.out.println("1. J20P3B2D5_01.txt (小规模)");
        System.out.println("2. J50P4B3D8_01.txt (中规模)");
        System.out.println("3. J100P5B4D10_01.txt (大规模)");
        System.out.println("4. 自定义路径");
        System.out.print("\n请输入选择 (1-4): ");
        
        String choice = scanner.nextLine().trim();
        
        String basePath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-3\\src\\main\\resources\\instance\\";
        
        switch (choice) {
            case "1":
                return basePath + "J20P3B2D5_01.txt";
            case "2":
                return basePath + "J50P4B3D8_01.txt";
            case "3":
                return basePath + "J100P5B4D10_01.txt";
            case "4":
                System.out.print("请输入算例文件路径: ");
                return scanner.nextLine().trim();
            default:
                System.out.println("无效选择，使用默认算例");
                return basePath + "J20P3B2D5_01.txt";
        }
    }
    
    /**
     * 提取算例名称
     */
    private static String extractInstanceName(String path) {
        String fileName = new File(path).getName();
        return fileName.replace(".txt", "");
    }
    
    /**
     * 加载问题
     */
    private static Problem loadProblem(String instancePath) throws Exception {
        File instanceFile = new File(instancePath);
        
        if (!instanceFile.exists()) {
            throw new FileNotFoundException("算例文件不存在: " + instancePath);
        }
        
        EnergyAwareInput input = new EnergyAwareInput(instanceFile);
        return input.getProblemDesFromFile();
    }
    
    /**
     * 配置目标函数
     * 
     * @param problem 问题实例
     * @param enableSwitchingStrategy 是否启用智能开关机策略
     */
    private static List<MOEvaluator.ObjectiveFunction> configureObjectives(
            Problem problem, boolean enableSwitchingStrategy) {
        List<MOEvaluator.ObjectiveFunction> objectives = new ArrayList<>();
        
        // 目标1: 最大完工时间
        objectives.add(new MOEvaluator.MaximumCompletionTime());
        
        // 目标2: 总能耗（可配置开关机策略）
        objectives.add(new MOEvaluator.TotalEnergyConsumption(
            problem, enableSwitchingStrategy, false));
        
        return objectives;
    }
    
    /**
     * 计算平均值
     */
    private static double calculateMean(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
    
    /**
     * 计算标准差
     */
    private static double calculateStdDev(List<Double> values, double mean) {
        if (values.size() <= 1) return 0.0;
        
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    /**
     * 重复字符串
     */
    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    // ==================== 结果导出方法 ====================
    
    /**
     * 导出Pareto前沿详细数据
     */
    private static void exportParetoFrontData(
            List<MOIndividual> paretoFront,
            String outputPath,
            String instanceName,
            String algorithm,
            int runNo) throws IOException {
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write("════════════════════════════════════════════════════════════════\n");
            writer.write("              Pareto前沿解集详细数据\n");
            writer.write("════════════════════════════════════════════════════════════════\n\n");
            
            writer.write("算例: " + instanceName + "\n");
            writer.write("算法: " + algorithm + "\n");
            writer.write("运行次数: " + runNo + "\n");
            writer.write("解的数量: " + paretoFront.size() + "\n\n");
            
            writer.write(repeatString("-", 100) + "\n");
            writer.write(String.format("%-8s %-20s %-20s %-15s %-10s\n",
                "序号", "Cmax", "能耗(kWh)", "装箱质量", "批次数"));
            writer.write(repeatString("-", 100) + "\n");
            
            int index = 1;
            for (MOIndividual ind : paretoFront) {
                writer.write(String.format("%-8d %-20.2f %-20.2f %-15.4f %-10d\n",
                    index++,
                    ind.objectives[0],
                    ind.objectives[1],
                    ind.packingQ,
                    ind.batchCount
                ));
            }
            
            writer.write(repeatString("-", 100) + "\n\n");
            
            // 统计信息
            double minCmax = paretoFront.stream().mapToDouble(i -> i.objectives[0]).min().orElse(0);
            double maxCmax = paretoFront.stream().mapToDouble(i -> i.objectives[0]).max().orElse(0);
            double minEnergy = paretoFront.stream().mapToDouble(i -> i.objectives[1]).min().orElse(0);
            double maxEnergy = paretoFront.stream().mapToDouble(i -> i.objectives[1]).max().orElse(0);
            
            writer.write("统计信息:\n");
            writer.write(String.format("  Cmax范围: [%.2f, %.2f]\n", minCmax, maxCmax));
            writer.write(String.format("  能耗范围: [%.2f, %.2f] kWh\n", minEnergy, maxEnergy));
            writer.write(String.format("  Cmax跨度: %.2f (%.2f%%)\n", 
                maxCmax - minCmax, (maxCmax - minCmax) / maxCmax * 100));
            writer.write(String.format("  能耗跨度: %.2f (%.2f%%)\n", 
                maxEnergy - minEnergy, (maxEnergy - minEnergy) / maxEnergy * 100));
            
            writer.write("\n════════════════════════════════════════════════════════════════\n");
        }
    }
    
    /**
     * 找到Cmax最小的解
     */
    private static MOIndividual findBestCmaxSolution(List<MOIndividual> paretoFront) {
        return paretoFront.stream()
            .min((a, b) -> Double.compare(a.objectives[0], b.objectives[0]))
            .orElse(paretoFront.get(0));
    }
    
    /**
     * 导出甘特图
     * 
     * 注意：为避免多线程数据竞争，每次调用都创建独立的operationMatrix
     */
    private static void exportGanttChart(
            MOIndividual individual,
            String outputPath,
            Problem problem,
            ProgramEntity.Operation[][] sharedOperationMatrix) throws IOException {
        
        // 创建独立的operationMatrix副本，避免多线程数据竞争
        int jobCount = problem.getJobCount();
        int[] opsCount = problem.getOperationCountArr();
        ProgramEntity.Operation[][] operationMatrix = new ProgramEntity.Operation[jobCount][];
        for (int i = 0; i < jobCount; i++) {
            operationMatrix[i] = new ProgramEntity.Operation[opsCount[i]];
            for (int j = 0; j < opsCount[i]; j++) {
                operationMatrix[i][j] = new ProgramEntity.Operation();
            }
        }
        
        // 转换为Chromosome并评估
        ProblemFrame.Chromosome chromosome = individual.toChromosome();
        
        // 使用CaculateFitness评估（会修改operationMatrix，但这是独立副本）
        ProblemFrame.CaculateFitness fitnessCalculator = new ProblemFrame.CaculateFitness();
        double makespan = fitnessCalculator.evaluate(chromosome, problem, operationMatrix);
        
        // 创建Solution对象
        ProblemFrame.Solution solution = new ProblemFrame.Solution(
            operationMatrix, chromosome, problem, makespan
        );
        
        // 使用ScheduleVisualizer生成甘特图
        AlgorithmFrame.visualization.ScheduleVisualizer visualizer = 
            new AlgorithmFrame.visualization.ScheduleVisualizer(
                solution, chromosome, problem, operationMatrix, null
            );
        visualizer.generateGanttChart(outputPath);
    }
    
    /**
     * 导出打印批次排布图
     * 
     * 注意：为避免多线程数据竞争，总是创建独立的operationMatrix进行评估
     */
    private static void exportPrinterLayout(
            MOIndividual individual,
            String outputPath,
            Problem problem) throws IOException {
        
        ProblemFrame.Chromosome chromosome = individual.toChromosome();
        
        // 创建独立的operationMatrix，避免多线程数据竞争
        // 即使printSolution已存在，也重新评估以确保数据一致性
        int jobCount = problem.getJobCount();
        int[] opsCount = problem.getOperationCountArr();
        ProgramEntity.Operation[][] operationMatrix = new ProgramEntity.Operation[jobCount][];
        for (int i = 0; i < jobCount; i++) {
            operationMatrix[i] = new ProgramEntity.Operation[opsCount[i]];
            for (int j = 0; j < opsCount[i]; j++) {
                operationMatrix[i][j] = new ProgramEntity.Operation();
            }
        }
        
        // 重新评估以生成完整的printSolution（使用独立的operationMatrix）
        ProblemFrame.CaculateFitness fitnessCalculator = new ProblemFrame.CaculateFitness();
        fitnessCalculator.evaluate(chromosome, problem, operationMatrix);
        
        // 使用PrinterLayoutVisualizer生成打印布局图
        AlgorithmFrame.visualization.PrinterLayoutVisualizer layoutVisualizer = 
            new AlgorithmFrame.visualization.PrinterLayoutVisualizer(
                chromosome, problem.getMachines()
            );
        layoutVisualizer.generatePrinterLayoutChart(outputPath);
    }
    
    /**
     * 导出详细调度记录
     * 
     * 注意：为避免多线程数据竞争，每次调用都创建独立的operationMatrix
     */
    private static void exportScheduleRecords(
            MOIndividual individual,
            String outputPath,
            Problem problem,
            ProgramEntity.Operation[][] sharedOperationMatrix) throws IOException {
        
        // 创建独立的operationMatrix副本，避免多线程数据竞争
        int jobCount = problem.getJobCount();
        int[] opsCount = problem.getOperationCountArr();
        ProgramEntity.Operation[][] operationMatrix = new ProgramEntity.Operation[jobCount][];
        for (int i = 0; i < jobCount; i++) {
            operationMatrix[i] = new ProgramEntity.Operation[opsCount[i]];
            for (int j = 0; j < opsCount[i]; j++) {
                operationMatrix[i][j] = new ProgramEntity.Operation();
            }
        }
        
        ProblemFrame.Chromosome chromosome = individual.toChromosome();
        
        // 评估（使用独立的operationMatrix）
        ProblemFrame.CaculateFitness fitnessCalculator = new ProblemFrame.CaculateFitness();
        fitnessCalculator.evaluate(chromosome, problem, operationMatrix);
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write("════════════════════════════════════════════════════════════════\n");
            writer.write("              最优Cmax解的详细调度记录\n");
            writer.write("════════════════════════════════════════════════════════════════\n\n");
            
            // 1. 基本信息
            writer.write("【基本信息】\n");
            writer.write(repeatString("-", 100) + "\n");
            writer.write(String.format("Cmax (最大完工时间): %.2f\n", individual.objectives[0]));
            writer.write(String.format("能耗: %.2f kWh\n", individual.objectives[1]));
            writer.write(String.format("装箱质量: %.4f\n", individual.packingQ));
            writer.write(String.format("批次总数: %d\n", individual.batchCount));
            writer.write(String.format("工件数量: %d\n", problem.getJobCount()));
            writer.write(String.format("机器数量: %d (打印机:%d, 批处理机:%d, 离散机:%d)\n\n",
                problem.getMachineCount(),
                problem.getPrintMachineCount(),
                problem.getBatchMachineCount(),
                problem.getMachineCount() - problem.getPrintMachineCount() - problem.getBatchMachineCount()
            ));
            
            // 2. 染色体编码
            writer.write("【染色体编码】\n");
            writer.write(repeatString("-", 100) + "\n");
            writer.write("工序序列 (OS): " + Arrays.toString(chromosome.gene_OS) + "\n");
            writer.write("机器选择 (MS): " + Arrays.toString(chromosome.gene_MS) + "\n\n");
            
            // 3. 零件加工顺序记录
            writer.write("【零件加工顺序记录】\n");
            writer.write(repeatString("-", 100) + "\n");
            exportJobProcessingOrder(writer, operationMatrix, problem);
            
            // 4. 机器加工顺序记录
            writer.write("\n【机器加工顺序记录】\n");
            writer.write(repeatString("-", 100) + "\n");
            exportMachineProcessingOrder(writer, operationMatrix, individual, problem);
            
            // 5. 打印批次详细信息
            writer.write("\n【打印批次详细信息】\n");
            writer.write(repeatString("-", 100) + "\n");
            exportPrintBatchDetails(writer, chromosome, problem);
            
            writer.write("\n════════════════════════════════════════════════════════════════\n");
        }
    }
    
    /**
     * 导出零件加工顺序记录
     */
    private static void exportJobProcessingOrder(
            BufferedWriter writer,
            ProgramEntity.Operation[][] operationMatrix,
            Problem problem) throws IOException {
        
        writer.write(String.format("%-8s %-15s %-15s %-15s %-15s %-15s\n",
            "工件号", "工序号", "机器号", "开始时间", "结束时间", "加工时长"));
        writer.write(repeatString("-", 100) + "\n");
        
        for (int jobIdx = 0; jobIdx < operationMatrix.length; jobIdx++) {
            writer.write(String.format("\n工件 %d:\n", jobIdx));
            
            for (int opIdx = 0; opIdx < operationMatrix[jobIdx].length; opIdx++) {
                ProgramEntity.Operation op = operationMatrix[jobIdx][opIdx];
                if (op != null) {
                    String machineType = getMachineTypeName(op.machineNo, problem);
                    writer.write(String.format("  %-8d %-15d %-15s %-15.2f %-15.2f %-15.2f\n",
                        op.jobNo,
                        opIdx,
                        machineType,
                        op.startTime,
                        op.endTime,
                        op.endTime - op.startTime
                    ));
                }
            }
        }
    }
    
    /**
     * 导出机器加工顺序记录
     */
    private static void exportMachineProcessingOrder(
            BufferedWriter writer,
            ProgramEntity.Operation[][] operationMatrix,
            MOIndividual individual,
            Problem problem) throws IOException {
        
        int machineCount = problem.getMachineCount();
        
        for (int machineIdx = 0; machineIdx < machineCount; machineIdx++) {
            String machineType = getMachineTypeName(machineIdx, problem);
            writer.write(String.format("\n%s:\n", machineType));
            writer.write(String.format("%-8s %-15s %-15s %-15s %-15s\n",
                "工件号", "工序号", "开始时间", "结束时间", "加工时长"));
            writer.write(repeatString("-", 80) + "\n");
            
            // 收集该机器上的所有工序
            List<ProgramEntity.Operation> machineOps = new ArrayList<>();
            for (int jobIdx = 0; jobIdx < operationMatrix.length; jobIdx++) {
                for (int opIdx = 0; opIdx < operationMatrix[jobIdx].length; opIdx++) {
                    ProgramEntity.Operation op = operationMatrix[jobIdx][opIdx];
                    if (op != null && op.machineNo == machineIdx) {
                        machineOps.add(op);
                    }
                }
            }
            
            // 按开始时间排序
            machineOps.sort((a, b) -> Double.compare(a.startTime, b.startTime));
            
            // 输出
            for (ProgramEntity.Operation op : machineOps) {
                writer.write(String.format("  %-8d %-15d %-15.2f %-15.2f %-15.2f\n",
                    op.jobNo,
                    op.task,
                    op.startTime,
                    op.endTime,
                    op.endTime - op.startTime
                ));
            }
            
            // 计算利用率
            double busyTime = machineOps.stream()
                .mapToDouble(op -> op.endTime - op.startTime)
                .sum();
            double totalTime = individual.objectives[0];  // Cmax
            double utilization = totalTime > 0 ? busyTime / totalTime * 100 : 0;
            
            writer.write(String.format("\n  机器利用率: %.2f%% (忙碌时间: %.2f / 总时间: %.2f)\n",
                utilization, busyTime, totalTime));
        }
    }
    
    /**
     * 导出打印批次详细信息
     */
    private static void exportPrintBatchDetails(
            BufferedWriter writer,
            ProblemFrame.Chromosome chromosome,
            Problem problem) throws IOException {
        
        if (chromosome.printSolution == null) {
            writer.write("  (无打印批次信息)\n");
            return;
        }
        
        int printMachineCount = problem.getPrintMachineCount();
        ProgramEntity.Item[] items = problem.getItems();
        
        for (int printerIdx = 0; printerIdx < printMachineCount; printerIdx++) {
            List<ProgramEntity.Solution> batches = chromosome.printSolution[printerIdx];
            if (batches == null || batches.isEmpty()) {
                continue;
            }
            
            ProgramEntity.Machine.PrintMachine printer = 
                (ProgramEntity.Machine.PrintMachine) problem.getMachines()[printerIdx];
            writer.write(String.format("\n打印机 %d (尺寸: %.1f x %.1f x %.1f):\n",
                printerIdx + 1, printer.L, printer.W, printer.H));
            writer.write(repeatString("-", 80) + "\n");
            
            for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
                ProgramEntity.Solution batch = batches.get(batchIdx);
                int itemCount = batch.placeItemList != null ? batch.placeItemList.size() : 0;
                writer.write(String.format("  批次 %d: 零件数=%d, 占用率=%.2f%%\n",
                    batchIdx + 1,
                    itemCount,
                    batch.rate * 100
                ));
                
                // 列出批次中的零件
                if (batch.placeItemList != null) {
                    for (ProgramEntity.PlaceItem placeItem : batch.placeItemList) {
                        String itemName = placeItem.name;
                        if (itemName != null && itemName.startsWith("Item")) {
                            try {
                                int itemIdx = Integer.parseInt(itemName.substring(4));
                                if (itemIdx >= 0 && itemIdx < items.length) {
                                    ProgramEntity.Item item = items[itemIdx];
                                    writer.write(String.format("    - 零件 %d: 尺寸 %.1f x %.1f x %.1f\n",
                                        itemIdx, item.l, item.w, item.h));
                                }
                            } catch (NumberFormatException e) {
                                writer.write(String.format("    - %s: 尺寸 %.1f x %.1f x %.1f\n",
                                    itemName, placeItem.l, placeItem.w, placeItem.h));
                            }
                        } else {
                            writer.write(String.format("    - %s: 尺寸 %.1f x %.1f x %.1f\n",
                                itemName != null ? itemName : "未知", placeItem.l, placeItem.w, placeItem.h));
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 获取机器类型名称
     */
    private static String getMachineTypeName(int machineIdx, Problem problem) {
        int printMachineCount = problem.getPrintMachineCount();
        int batchMachineCount = problem.getBatchMachineCount();
        
        if (machineIdx < printMachineCount) {
            return "打印机" + (machineIdx + 1);
        } else if (machineIdx < printMachineCount + batchMachineCount) {
            return "批处理机" + (machineIdx - printMachineCount + 1);
        } else {
            return "离散机" + (machineIdx - printMachineCount - batchMachineCount + 1);
        }
    }
    
    /**
     * 打印欢迎信息
     */
    private static void printWelcome() {
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                ║");
        System.out.println("║          算法对比实验框架 v1.3                                 ║");
        System.out.println("║                                                                ║");
        System.out.println("║  对比算法: NSGA-II, NSGA-II-Basic, MOEA/D, MOGWO, SPEA2      ║");
        System.out.println("║  评价指标: HV, IGD, C-metric                                  ║");
        System.out.println("║  重复次数: 10次                                                ║");
        System.out.println("║                                                                ║");
        System.out.println("║  配置说明:                                                     ║");
        System.out.println("║    - NSGA-II:       启发式初始化 + 局部搜索 (改进版)         ║");
        System.out.println("║    - 其他算法:      标准配置 + 无局部搜索 (基准版)           ║");
        System.out.println("║                                                                ║");
        System.out.println("║  实验目的: 证明NSGA-II改进策略的有效性                        ║");
        System.out.println("║                                                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 归一化信息
     */
    private static class NormalizationInfo {
        double[] minValues;
        double[] maxValues;
        
        NormalizationInfo(double[] minValues, double[] maxValues) {
            this.minValues = minValues;
            this.maxValues = maxValues;
        }
    }
    
    /**
     * 算法结果
     */
    private static class AlgorithmResults {
        List<List<MOIndividual>> paretoFronts;
        List<Double> runTimes;
        Problem problem;  // 保存问题实例，用于生成图表
        ProgramEntity.Operation[][] operationMatrix;  // 保存工序矩阵，用于生成图表
    }
    
    /**
     * 性能指标
     */
    private static class PerformanceMetrics {
        double avgHV, stdHV, minHV, maxHV;
        double avgIGD, stdIGD, minIGD, maxIGD;
        List<Double> hvValues;
        List<Double> igdValues;
    }
    
    /**
     * 算法任务（多线程）
     * 
     * 线程安全设计：
     * - 每个线程创建独立的objectives实例
     * - Problem对象只读，多线程共享安全
     * - 避免共享可变状态
     */
    private static class AlgorithmTask implements Callable<AlgorithmTask.Result> {
        
        private final String algorithm;
        private final Problem problem;
        private final boolean enableSwitchingStrategy;  // 改为保存配置
        private final int runNo;
        
        public AlgorithmTask(String algorithm,
                           Problem problem,
                           boolean enableSwitchingStrategy,
                           int runNo) {
            this.algorithm = algorithm;
            this.problem = problem;
            this.enableSwitchingStrategy = enableSwitchingStrategy;
            this.runNo = runNo;
        }
        
        @Override
        public Result call() {
            long startTime = System.currentTimeMillis();
            
            try {
                // ✅ 为每个线程创建独立的objectives实例，避免共享
                List<MOEvaluator.ObjectiveFunction> objectives = 
                    configureObjectives(problem, enableSwitchingStrategy);
                
                List<MOIndividual> paretoFront = runSingleAlgorithm(
                    algorithm, problem, objectives, runNo
                );
                
                long endTime = System.currentTimeMillis();
                double runTime = (endTime - startTime) / 1000.0;
                
                return new Result(paretoFront, runTime);
                
            } catch (Exception e) {
                System.err.println("\n❌ " + algorithm + " 运行" + runNo + "失败: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
        
        static class Result {
            List<MOIndividual> paretoFront;
            double runTime;
            
            Result(List<MOIndividual> paretoFront, double runTime) {
                this.paretoFront = paretoFront;
                this.runTime = runTime;
            }
        }
    }
}
