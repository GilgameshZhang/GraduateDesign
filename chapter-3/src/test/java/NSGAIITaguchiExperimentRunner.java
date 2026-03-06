import AlgorithmFrame.nsgaii.NSGAII;
import AlgorithmFrame.nsgaii.NSGAIIOperations;
import ProblemFrame.MOEvaluator;
import ProblemFrame.MOIndividual;
import ProgramEntity.EnergyAwareInput;
import ProgramEntity.Problem;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NSGA-II 田口实验运行器
 * 
 * 功能：
 * 1. 使用L9正交表设计进行参数调优
 * 2. 使用超体积(HV)作为评价指标
 * 3. 每组参数运行10次取平均值
 * 4. 生成详细的实验报告
 * 
 * 田口实验设计：
 * - 因子A: 种群规模 (popSize)
 * - 因子B: 交叉率 (crossoverRate)
 * - 因子C: 变异率 (mutationRate)
 * - 因子D: 每代搜索次数 (localSearchL - 每个精英的尝试次数)
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class NSGAIITaguchiExperimentRunner {
    
    // ==================== 实验配置 ====================
    
    /** 每组参数重复次数 */
    private static final int REPEAT_TIMES = 10;
    
    /** 最大运行时间（分钟） */
    private static final double MAX_RUN_TIME = 5.0;
    
    /** 最大代数 */
    private static final int MAX_GENERATIONS = 5000000;
    
    /** 是否启用局部搜索 */
    private static final boolean ENABLE_LOCAL_SEARCH = true;
    
    /** 是否启用多线程 */
    private static final boolean ENABLE_MULTI_THREAD = true;
    
    /** 线程池大小（0表示自动检测CPU核心数） */
    private static final int THREAD_POOL_SIZE = 0;
    
    /** 输出目录 */
    private static final String OUTPUT_DIR = "chapter-3/output/taguchi_experiment/";
    
    // ==================== 主程序 ====================
    
    public static void main(String[] args) {
        try {
            // 打印欢迎信息
            printWelcome();
            
            // 选择算例
            String instancePath = selectInstance();
            String instanceName = extractInstanceName(instancePath);
            
            System.out.println("\n【算例文件】" + instancePath);
            System.out.println("【算例名称】" + instanceName);
            
            // 加载算例
            System.out.println("\n════════════════════════════════════════");
            System.out.println("  加载算例");
            System.out.println("════════════════════════════════════════\n");
            
            Problem problem = loadProblem(instancePath);
            System.out.println("✅ 算例加载成功");
            System.out.println("   工件数: " + problem.getJobCount());
            System.out.println("   机器数: " + problem.getMachineCount());
            
            // 配置目标函数
            List<MOEvaluator.ObjectiveFunction> objectives = configureObjectives(problem);
            
            // 运行田口实验（两阶段法）
            System.out.println("\n════════════════════════════════════════");
            System.out.println("  田口实验 - L9正交表设计");
            System.out.println("════════════════════════════════════════\n");
            System.out.println("策略: 两阶段法");
            System.out.println("  阶段1: 运行所有实验，记录Pareto前沿");
            System.out.println("  阶段2: 根据所有实验确定全局参考点");
            System.out.println("  阶段3: 使用统一参考点重新计算所有HV值\n");
            
            runTaguchiExperiment(problem, objectives, instanceName);
            
            System.out.println("\n✅ 所有实验完成！");
            
        } catch (Exception e) {
            System.err.println("\n❌ 程序执行出错:");
            System.err.println("   " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 运行田口实验（两阶段法）
     */
    private static void runTaguchiExperiment(Problem problem, 
                                            List<MOEvaluator.ObjectiveFunction> objectives,
                                            String instanceName) throws IOException {
        
        // L9正交表设计 (4因子3水平)
        // 因子: A=popSize, B=crossoverRate, C=mutationRate, D=localSearchL
        int[][] l9OrthogonalArray = {
            // A     B    C    D
            {  50,  70,  10,  5},  // 实验1: 水平 1-1-1-1
            {  50,  80,  20, 10},  // 实验2: 水平 1-2-2-2
            {  50,  90,  30, 20},  // 实验3: 水平 1-3-3-3
            { 100,  70,  20, 20},  // 实验4: 水平 2-1-2-3
            { 100,  80,  30,  5},  // 实验5: 水平 2-2-3-1
            { 100,  90,  10, 10},  // 实验6: 水平 2-3-1-2
            { 150,  70,  30, 10},  // 实验7: 水平 3-1-3-2
            { 150,  80,  10, 20},  // 实验8: 水平 3-2-1-3
            { 150,  90,  20,  5}   // 实验9: 水平 3-3-2-1
        };
        
        printExperimentDesign();
        
        // 【阶段1】存储所有实验的Pareto前沿
        List<List<List<MOIndividual>>> allParetoFronts = new ArrayList<>();  // 9组实验，每组10次
        List<double[]> allRunTimes = new ArrayList<>();
        
        System.out.println("\n════════════════════════════════════════════════════════════════");
        System.out.println("  阶段1: 运行所有实验并记录Pareto前沿");
        System.out.println("════════════════════════════════════════════════════════════════");
        
        // 多线程配置
        if (ENABLE_MULTI_THREAD) {
            int threadCount = THREAD_POOL_SIZE > 0 ? THREAD_POOL_SIZE : 
                             Runtime.getRuntime().availableProcessors();
            System.out.println("\n多线程模式: 启用");
            System.out.println("  CPU核心数: " + Runtime.getRuntime().availableProcessors());
            System.out.println("  使用线程数: " + threadCount);
            System.out.println("  总任务数: " + (l9OrthogonalArray.length * REPEAT_TIMES) + " 个");
            System.out.println("  预计加速比: " + Math.min(threadCount, 
                             l9OrthogonalArray.length * REPEAT_TIMES) + "x\n");
        } else {
            System.out.println("\n单线程模式\n");
        }
        
        // 运行9组实验（支持多线程）
        if (ENABLE_MULTI_THREAD) {
            // 多线程模式
            runAllExperimentsMultiThread(problem, objectives, instanceName, 
                                        l9OrthogonalArray, allParetoFronts, allRunTimes);
        } else {
            // 单线程模式
            runAllExperimentsSingleThread(problem, objectives, instanceName, 
                                         l9OrthogonalArray, allParetoFronts, allRunTimes);
        }
        
        // 【阶段2】计算归一化范围并进行归一化
        System.out.println("\n════════════════════════════════════════════════════════════════");
        System.out.println("  阶段2: 归一化处理");
        System.out.println("════════════════════════════════════════════════════════════════\n");
        
        NormalizationInfo normInfo = calculateNormalizationRange(allParetoFronts);
        
        System.out.println("✅ 归一化范围确定完成");
        System.out.println("   Cmax范围:   [" + String.format("%.2f", normInfo.minValues[0]) + 
                         ", " + String.format("%.2f", normInfo.maxValues[0]) + "]");
        System.out.println("   Energy范围: [" + String.format("%.2f", normInfo.minValues[1]) + 
                         ", " + String.format("%.2f", normInfo.maxValues[1]) + "]");
        System.out.println("\n说明: 基于所有 " + (l9OrthogonalArray.length * REPEAT_TIMES) + " 次实验");
        System.out.println("      目标值将归一化到 [0, 1] 区间");
        System.out.println("      使用固定参考点 (1.1, 1.1) 计算HV\n");
        
        // 【阶段3】归一化并计算HV值
        System.out.println("\n════════════════════════════════════════════════════════════════");
        System.out.println("  阶段3: 归一化并计算HV值");
        System.out.println("════════════════════════════════════════════════════════════════\n");
        
        // 固定的归一化参考点
        final double[] normalizedReferencePoint = {1.1, 1.1};
        
        System.out.println("归一化参考点: (" + normalizedReferencePoint[0] + ", " + 
                         normalizedReferencePoint[1] + ")");
        System.out.println("说明: 所有目标值归一化到[0,1]后，使用统一参考点(1.1, 1.1)\n");
        
        // 存储最终结果
        double[] avgHVResults = new double[9];
        double[] stdHVResults = new double[9];
        double[] minHVResults = new double[9];
        double[] maxHVResults = new double[9];
        double[] avgParetoSizes = new double[9];
        List<List<Double>> allHVData = new ArrayList<>();
        
        for (int i = 0; i < l9OrthogonalArray.length; i++) {
            List<List<MOIndividual>> experimentParetoFronts = allParetoFronts.get(i);
            List<Double> hvValues = new ArrayList<>();
            int totalParetoSize = 0;
            
            for (List<MOIndividual> paretoFront : experimentParetoFronts) {
                // 归一化Pareto前沿
                List<MOIndividual> normalizedFront = normalizeParetoFront(
                    paretoFront, normInfo.minValues, normInfo.maxValues
                );
                
                // 使用归一化的参考点计算HV
                double hv = NSGAIIOperations.calculateHypervolume2D(
                    normalizedFront, normalizedReferencePoint
                );
                
                hvValues.add(hv);
                totalParetoSize += paretoFront.size();
            }
            
            // 计算统计量
            avgHVResults[i] = calculateMean(hvValues);
            stdHVResults[i] = calculateStdDev(hvValues, avgHVResults[i]);
            minHVResults[i] = Collections.min(hvValues);
            maxHVResults[i] = Collections.max(hvValues);
            avgParetoSizes[i] = (double) totalParetoSize / experimentParetoFronts.size();
            allHVData.add(hvValues);
            
            System.out.println("实验组 " + (i+1) + ": 平均HV = " + String.format("%.6f", avgHVResults[i]) + 
                             " (标准差: " + String.format("%.6f", stdHVResults[i]) + ")");
        }
        
        System.out.println("\n✅ 所有HV值计算完成\n");
        
        // 生成L9实验总结报告
        generateL9SummaryReport(
            instanceName, l9OrthogonalArray, 
            avgHVResults, stdHVResults, minHVResults, maxHVResults, 
            avgParetoSizes, allHVData, normInfo
        );
        
        // 输出最终总结
        printFinalSummary(avgHVResults);
    }
    
    /**
     * 多线程模式：运行所有实验
     */
    private static void runAllExperimentsMultiThread(
            Problem problem,
            List<MOEvaluator.ObjectiveFunction> objectives,
            String instanceName,
            int[][] l9Array,
            List<List<List<MOIndividual>>> allParetoFronts,
            List<double[]> allRunTimes) {
        
        int threadCount = THREAD_POOL_SIZE > 0 ? THREAD_POOL_SIZE : 
                         Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // 任务计数器
        AtomicInteger completedTasks = new AtomicInteger(0);
        int totalTasks = l9Array.length * REPEAT_TIMES;
        
        // 存储结果（需要线程安全）
        ConcurrentHashMap<String, ExperimentTask.Result> results = new ConcurrentHashMap<>();
        
        // 提交所有任务
        List<Future<ExperimentTask.Result>> futures = new ArrayList<>();
        
        for (int expGroup = 0; expGroup < l9Array.length; expGroup++) {
            int[] config = l9Array[expGroup];
            
            for (int run = 1; run <= REPEAT_TIMES; run++) {
                ExperimentTask task = new ExperimentTask(
                    problem, objectives, instanceName,
                    expGroup + 1, run, config,
                    completedTasks, totalTasks
                );
                futures.add(executor.submit(task));
            }
        }
        
        // 等待所有任务完成
        System.out.println("所有任务已提交，等待完成...\n");
        
        try {
            for (Future<ExperimentTask.Result> future : futures) {
                ExperimentTask.Result result = future.get();
                results.put(result.key, result);
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ 任务执行出错: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
        
        System.out.println("\n✅ 所有实验运行完成！\n");
        
        // 整理结果
        for (int i = 0; i < l9Array.length; i++) {
            List<List<MOIndividual>> groupFronts = new ArrayList<>();
            double[] groupTimes = new double[REPEAT_TIMES];
            
            for (int run = 1; run <= REPEAT_TIMES; run++) {
                String key = "exp" + (i + 1) + "_run" + run;
                ExperimentTask.Result result = results.get(key);
                if (result != null) {
                    groupFronts.add(result.paretoFront);
                    groupTimes[run - 1] = result.runTime;
                }
            }
            
            allParetoFronts.add(groupFronts);
            allRunTimes.add(groupTimes);
            
            // 打印组总结
            double avgParetoSize = groupFronts.stream()
                .mapToInt(List::size)
                .average()
                .orElse(0.0);
            double avgTime = Arrays.stream(groupTimes).average().orElse(0.0);
            
            System.out.println("实验组 " + (i + 1) + " - 平均Pareto规模: " + 
                             String.format("%.2f", avgParetoSize) + 
                             ", 平均运行时间: " + String.format("%.2f", avgTime) + "秒");
        }
    }
    
    /**
     * 单线程模式：运行所有实验
     */
    private static void runAllExperimentsSingleThread(
            Problem problem,
            List<MOEvaluator.ObjectiveFunction> objectives,
            String instanceName,
            int[][] l9Array,
            List<List<List<MOIndividual>>> allParetoFronts,
            List<double[]> allRunTimes) {
        
        for (int i = 0; i < l9Array.length; i++) {
            int[] config = l9Array[i];
            
            System.out.println("\n████████████████████████████████████████████████████████████████");
            System.out.println("                   实验组 " + (i+1) + "/9");
            System.out.println("████████████████████████████████████████████████████████████████");
            System.out.println("参数配置:");
            System.out.println("  种群规模:       " + config[0]);
            System.out.println("  交叉率:         " + (config[1] / 100.0));
            System.out.println("  变异率:         " + (config[2] / 100.0));
            System.out.println("  每代搜索次数L:  " + config[3]);
            System.out.println("  重复次数:       " + REPEAT_TIMES);
            System.out.println("════════════════════════════════════════════════════════════════\n");
            
            // 运行多次实验，记录Pareto前沿
            ExperimentRawData rawData = runMultipleExperimentsPhase1(
                problem, objectives, instanceName, 
                i + 1, config, REPEAT_TIMES
            );
            
            allParetoFronts.add(rawData.paretoFronts);
            allRunTimes.add(rawData.runTimes);
            
            // 打印本组总结
            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("  实验组 " + (i+1) + " 完成");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("平均Pareto前沿规模: " + String.format("%.2f", rawData.avgParetoSize));
            System.out.println("平均运行时间: " + String.format("%.2f", rawData.avgRunTime) + " 秒");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        }
    }
    
    /**
     * 阶段1：运行多次实验并记录Pareto前沿（单线程版本）
     */
    private static ExperimentRawData runMultipleExperimentsPhase1(
            Problem problem,
            List<MOEvaluator.ObjectiveFunction> objectives,
            String instanceName,
            int experimentNo,
            int[] config,
            int repeatTimes) {
        
        List<List<MOIndividual>> paretoFronts = new ArrayList<>();
        List<Integer> paretoSizes = new ArrayList<>();
        double[] runTimesArray = new double[repeatTimes];
        
        for (int run = 1; run <= repeatTimes; run++) {
            System.out.println("──────────────────────────────────────");
            System.out.println("  第 " + experimentNo + " 组 - 运行 " + run + "/" + repeatTimes);
            System.out.println("──────────────────────────────────────");
            
            // 创建NSGA-II实例
            NSGAII nsgaii = new NSGAII(problem, objectives);
            
            // 设置参数
            nsgaii.setPopulationSize(config[0]);               // popSize
            nsgaii.setCrossoverRate(config[1] / 100.0);        // crossoverRate
            nsgaii.setMutationRate(config[2] / 100.0);         // mutationRate
            nsgaii.setMaxGenerations(MAX_GENERATIONS);
            nsgaii.setMaxRunTimeMinutes(MAX_RUN_TIME);
            
            // 局部搜索 - 每代都执行，设置每个精英的尝试次数L
            if (ENABLE_LOCAL_SEARCH) {
                nsgaii.enableLocalSearch(true, 1);             // interval=1表示每代都执行
                nsgaii.setLocalSearchParameters(
                    config[3],    // L - 每个精英的尝试次数
                    0.10,         // eta - 精英比例10%
                    0.05,         // epsE - T型允许能耗上升5%
                    0.02,         // epsC - E型允许Cmax上升2%
                    0.005,        // improvC - T型要求Cmax下降0.5%
                    0.01          // improvE - E型要求能耗下降1%
                );
            }
            
            // 设置随机种子（保证可重复性，但每次运行不同）
            nsgaii.setSeed(System.currentTimeMillis() + run);
            
            // 运行算法
            long startTime = System.currentTimeMillis();
            List<MOIndividual> paretoFront = nsgaii.solve();
            long endTime = System.currentTimeMillis();
            double runTime = (endTime - startTime) / 1000.0;
            
            // 记录Pareto前沿和基本信息
            paretoFronts.add(paretoFront);
            paretoSizes.add(paretoFront.size());
            runTimesArray[run - 1] = runTime;
            
            System.out.println("✅ 运行 " + run + " 完成:");
            System.out.println("   Pareto前沿规模 = " + paretoFront.size());
            System.out.println("   运行时间 = " + String.format("%.2f", runTime) + " 秒\n");
        }
        
        // 计算统计量
        ExperimentRawData rawData = new ExperimentRawData();
        rawData.paretoFronts = paretoFronts;
        rawData.runTimes = runTimesArray;
        rawData.avgParetoSize = calculateMean(paretoSizes.stream()
            .mapToDouble(Integer::doubleValue).boxed()
            .collect(java.util.stream.Collectors.toList()));
        rawData.avgRunTime = 0;
        for (double t : runTimesArray) rawData.avgRunTime += t;
        rawData.avgRunTime /= runTimesArray.length;
        
        return rawData;
    }
    
    /**
     * 生成L9实验总结报告
     */
    private static void generateL9SummaryReport(
            String instanceName,
            int[][] l9Array,
            double[] avgHV,
            double[] stdHV,
            double[] minHV,
            double[] maxHV,
            double[] avgParetoSize,
            List<List<Double>> allHVData,
            NormalizationInfo normInfo) throws IOException {
        
        // 创建输出目录
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String outputDir = OUTPUT_DIR + instanceName + "_" + timestamp + "/";
        new File(outputDir).mkdirs();
        
        String reportFile = outputDir + "L9实验总结报告.txt";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
            // 标题
            writer.println("════════════════════════════════════════════════════════════════");
            writer.println("              NSGA-II 田口实验总结报告");
            writer.println("════════════════════════════════════════════════════════════════");
            writer.println();
            writer.println("生成时间: " + new Date());
            writer.println("算例名称: " + instanceName);
            writer.println("实验设计: L9(3^4) 正交表");
            writer.println("评价指标: 超体积 (Hypervolume, HV)");
            writer.println("重复次数: " + REPEAT_TIMES + " 次");
            writer.println();
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  归一化范围与参考点");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println();
            writer.println("原始目标值范围:");
            writer.println("  Cmax:   [" + String.format("%.2f", normInfo.minValues[0]) + 
                         ", " + String.format("%.2f", normInfo.maxValues[0]) + "]");
            writer.println("  Energy: [" + String.format("%.2f", normInfo.minValues[1]) + 
                         ", " + String.format("%.2f", normInfo.maxValues[1]) + "]");
            writer.println();
            writer.println("归一化参考点: (1.1, 1.1)");
            writer.println();
            writer.println("计算方法: 两阶段法 + 归一化");
            writer.println("  1. 运行所有L9实验（9组×" + REPEAT_TIMES + "次 = " + 
                         (9 * REPEAT_TIMES) + "次）");
            writer.println("  2. 从所有Pareto前沿找出各目标的最小值和最大值");
            writer.println("  3. 将所有目标值归一化到[0, 1]区间");
            writer.println("     归一化公式: (value - min) / (max - min)");
            writer.println("  4. 使用固定参考点(1.1, 1.1)计算所有实验的HV值");
            writer.println();
            writer.println("优势: ");
            writer.println("  - 归一化消除了不同目标的量纲差异");
            writer.println("  - 固定参考点(1.1, 1.1)确保不同算例之间HV可比");
            writer.println("  - 基于所有实际数据，最准确可靠");
            writer.println("  - HV值范围在[0, 1.21]之间，越大越好");
            writer.println();
            
            // 因子水平表
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  因子水平表");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println();
            writer.println("因子A - 种群规模 (popSize):");
            writer.println("  水平1: 50");
            writer.println("  水平2: 100");
            writer.println("  水平3: 150");
            writer.println();
            writer.println("因子B - 交叉率 (crossoverRate):");
            writer.println("  水平1: 0.70");
            writer.println("  水平2: 0.80");
            writer.println("  水平3: 0.90");
            writer.println();
            writer.println("因子C - 变异率 (mutationRate):");
            writer.println("  水平1: 0.10");
            writer.println("  水平2: 0.20");
            writer.println("  水平3: 0.30");
            writer.println();
            writer.println("因子D - 每代搜索次数 (localSearchL - 每个精英的尝试次数):");
            writer.println("  水平1: 5");
            writer.println("  水平2: 10");
            writer.println("  水平3: 20");
            writer.println();
            
            // L9实验结果表
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  L9实验结果表");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println();
            writer.println(String.format("%-6s %-8s %-10s %-10s %-10s %-12s %-12s %-12s %-12s %-12s",
                "实验", "A", "B", "C", "D", "平均HV", "标准差", "最大HV", "最小HV", "Pareto规模"));
            writer.println(repeatString("─", 120));
            
            for (int i = 0; i < 9; i++) {
                int[] config = l9Array[i];
                writer.println(String.format("%-6d %-8d %-10.2f %-10.2f %-10d %-12.6f %-12.6f %-12.6f %-12.6f %-12.2f",
                    i + 1,
                    config[0],
                    config[1] / 100.0,
                    config[2] / 100.0,
                    config[3],
                    avgHV[i],
                    stdHV[i],
                    maxHV[i],
                    minHV[i],
                    avgParetoSize[i]
                ));
            }
            writer.println();
            
            // 因子效应分析
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  因子效应分析 (基于平均HV)");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println();
            
            // 计算因子效应
            double[][] factorEffects = calculateFactorEffects(l9Array, avgHV);
            
            String[] factorNames = {"A (popSize)", "B (crossoverRate)", "C (mutationRate)", "D (localSearchL)"};
            
            for (int factor = 0; factor < 4; factor++) {
                writer.println("因子 " + factorNames[factor] + ":");
                writer.println("  水平1平均HV: " + String.format("%.6f", factorEffects[factor][0]));
                writer.println("  水平2平均HV: " + String.format("%.6f", factorEffects[factor][1]));
                writer.println("  水平3平均HV: " + String.format("%.6f", factorEffects[factor][2]));
                
                double range = Math.max(factorEffects[factor][0], 
                               Math.max(factorEffects[factor][1], factorEffects[factor][2])) -
                               Math.min(factorEffects[factor][0], 
                               Math.min(factorEffects[factor][1], factorEffects[factor][2]));
                
                writer.println("  极差 (Range): " + String.format("%.6f", range));
                
                // 找出最优水平
                int bestLevel = 0;
                double bestValue = factorEffects[factor][0];
                for (int level = 1; level < 3; level++) {
                    if (factorEffects[factor][level] > bestValue) {
                        bestValue = factorEffects[factor][level];
                        bestLevel = level;
                    }
                }
                writer.println("  最优水平: 水平" + (bestLevel + 1) + " (HV = " + String.format("%.6f", bestValue) + ")");
                writer.println();
            }
            
            // 推荐参数配置
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  推荐参数配置");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println();
            writer.println("基于因子效应分析，推荐以下参数配置：");
            writer.println();
            
            int[] recommendedConfig = getRecommendedConfig(factorEffects);
            writer.println("  种群规模:       " + getPopSizeValue(recommendedConfig[0]));
            writer.println("  交叉率:         " + getCrossoverRateValue(recommendedConfig[1]));
            writer.println("  变异率:         " + getMutationRateValue(recommendedConfig[2]));
            writer.println("  每代搜索次数L:  " + getLocalSearchLValue(recommendedConfig[3]));
            writer.println();
            
            // 实验结果排名
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  实验结果排名 (按平均HV降序)");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println();
            
            // 创建排序索引
            Integer[] indices = new Integer[9];
            for (int i = 0; i < 9; i++) indices[i] = i;
            Arrays.sort(indices, (a, b) -> Double.compare(avgHV[b], avgHV[a]));
            
            writer.println(String.format("%-6s %-8s %-12s %-8s %-10s %-10s %-10s",
                "排名", "实验号", "平均HV", "A", "B", "C", "D"));
            writer.println(repeatString("─", 70));
            
            for (int rank = 0; rank < 9; rank++) {
                int expNo = indices[rank];
                int[] config = l9Array[expNo];
                writer.println(String.format("%-6d %-8d %-12.6f %-8d %-10.2f %-10.2f %-10d",
                    rank + 1,
                    expNo + 1,
                    avgHV[expNo],
                    config[0],
                    config[1] / 100.0,
                    config[2] / 100.0,
                    config[3]
                ));
            }
            writer.println();
            
            // 详细数据（每次运行的HV值）- 便于复制到Excel
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  详细数据 (每次运行的HV值) - 便于复制到Excel");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println();
            writer.println("格式说明: 每行包含一个实验组的所有运行结果");
            writer.println("列: 实验号, 运行1, 运行2, ..., 运行10, 平均值, 标准差");
            writer.println();
            
            // 表头
            writer.print(String.format("%-8s", "实验号"));
            for (int j = 1; j <= REPEAT_TIMES; j++) {
                writer.print(String.format("%-12s", "运行" + j));
            }
            writer.print(String.format("%-12s", "平均值"));
            writer.print(String.format("%-12s", "标准差"));
            writer.println();
            writer.println(repeatString("─", 8 + REPEAT_TIMES * 12 + 24));
            
            // 数据行
            for (int i = 0; i < 9; i++) {
                List<Double> hvData = allHVData.get(i);
                
                // 实验号
                writer.print(String.format("%-8d", i + 1));
                
                // 每次运行的HV值
                for (int j = 0; j < hvData.size(); j++) {
                    writer.print(String.format("%-12.6f", hvData.get(j)));
                }
                
                // 平均值和标准差
                writer.print(String.format("%-12.6f", avgHV[i]));
                writer.print(String.format("%-12.6f", stdHV[i]));
                writer.println();
            }
            writer.println();
            
            // CSV格式输出（更便于Excel导入）
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  CSV格式数据（可直接复制到Excel）");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println();
            
            // CSV表头
            writer.print("实验号");
            for (int j = 1; j <= REPEAT_TIMES; j++) {
                writer.print(",运行" + j);
            }
            writer.print(",平均值,标准差,最大值,最小值");
            writer.println();
            
            // CSV数据
            for (int i = 0; i < 9; i++) {
                List<Double> hvData = allHVData.get(i);
                
                writer.print(i + 1);
                for (Double hv : hvData) {
                    writer.print("," + String.format("%.6f", hv));
                }
                writer.print("," + String.format("%.6f", avgHV[i]));
                writer.print("," + String.format("%.6f", stdHV[i]));
                writer.print("," + String.format("%.6f", maxHV[i]));
                writer.print("," + String.format("%.6f", minHV[i]));
                writer.println();
            }
            writer.println();
            
            // 结束
            writer.println("════════════════════════════════════════════════════════════════");
            writer.println("                      报告结束");
            writer.println("════════════════════════════════════════════════════════════════");
        }
        
        System.out.println("\n📊 总结报告已保存到: " + reportFile);
    }
    
    /**
     * 计算因子效应
     */
    private static double[][] calculateFactorEffects(int[][] l9Array, double[] avgHV) {
        double[][] effects = new double[4][3]; // 4个因子，每个3个水平
        int[][] counts = new int[4][3];
        
        // 映射参数值到水平
        Map<Integer, Integer> popSizeToLevel = new HashMap<>();
        popSizeToLevel.put(50, 0);
        popSizeToLevel.put(100, 1);
        popSizeToLevel.put(150, 2);
        
        Map<Integer, Integer> crossoverToLevel = new HashMap<>();
        crossoverToLevel.put(70, 0);
        crossoverToLevel.put(80, 1);
        crossoverToLevel.put(90, 2);
        
        Map<Integer, Integer> mutationToLevel = new HashMap<>();
        mutationToLevel.put(10, 0);
        mutationToLevel.put(20, 1);
        mutationToLevel.put(30, 2);
        
        Map<Integer, Integer> localSearchToLevel = new HashMap<>();
        localSearchToLevel.put(5, 0);
        localSearchToLevel.put(10, 1);
        localSearchToLevel.put(20, 2);
        
        // 累加各水平的HV值
        for (int i = 0; i < 9; i++) {
            int[] config = l9Array[i];
            
            int levelA = popSizeToLevel.get(config[0]);
            int levelB = crossoverToLevel.get(config[1]);
            int levelC = mutationToLevel.get(config[2]);
            int levelD = localSearchToLevel.get(config[3]);
            
            effects[0][levelA] += avgHV[i];
            counts[0][levelA]++;
            
            effects[1][levelB] += avgHV[i];
            counts[1][levelB]++;
            
            effects[2][levelC] += avgHV[i];
            counts[2][levelC]++;
            
            effects[3][levelD] += avgHV[i];
            counts[3][levelD]++;
        }
        
        // 计算平均值
        for (int factor = 0; factor < 4; factor++) {
            for (int level = 0; level < 3; level++) {
                if (counts[factor][level] > 0) {
                    effects[factor][level] /= counts[factor][level];
                }
            }
        }
        
        return effects;
    }
    
    /**
     * 获取推荐配置
     */
    private static int[] getRecommendedConfig(double[][] factorEffects) {
        int[] config = new int[4];
        
        for (int factor = 0; factor < 4; factor++) {
            int bestLevel = 0;
            double bestValue = factorEffects[factor][0];
            
            for (int level = 1; level < 3; level++) {
                if (factorEffects[factor][level] > bestValue) {
                    bestValue = factorEffects[factor][level];
                    bestLevel = level;
                }
            }
            
            config[factor] = bestLevel;
        }
        
        return config;
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 选择算例
     */
    private static String selectInstance() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("请选择算例:");
        System.out.println("1. J20P3B2D5_01.txt (小规模)");
        System.out.println("2. J50P5B4D10_02.txt (中规模)");
        System.out.println("3. J100P5B4D10_05.txt (大规模)");
        System.out.println("4. 自定义路径");
        System.out.print("\n请输入选择 (1-4): ");
        
        String choice = scanner.nextLine().trim();
        
        String basePath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-3\\src\\main\\resources\\instance\\";
        
        switch (choice) {
            case "1":
                return basePath + "J20P3B2D5_01.txt";
            case "2":
                return basePath + "J50P5B4D10_02.txt";
            case "3":
                return basePath + "J100P5B4D10_05.txt";
            case "4":
                System.out.print("请输入算例文件路径: ");
                return scanner.nextLine().trim();
            default:
                System.out.println("无效选择，使用默认算例 J20P3B2D5_01.txt");
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
     */
    private static List<MOEvaluator.ObjectiveFunction> configureObjectives(Problem problem) {
        List<MOEvaluator.ObjectiveFunction> objectives = new ArrayList<>();
        
        // 目标1: 最大完工时间
        objectives.add(new MOEvaluator.MaximumCompletionTime());
        
        // 目标2: 总能耗（智能开关机策略）
        objectives.add(new MOEvaluator.TotalEnergyConsumption(problem, true, false));
        
        return objectives;
    }
    
    /**
     * 计算归一化范围（最小值和最大值）
     * 
     * @param allParetoFronts 所有实验的Pareto前沿
     * @return 归一化信息
     */
    private static NormalizationInfo calculateNormalizationRange(
            List<List<List<MOIndividual>>> allParetoFronts) {
        
        System.out.println("正在分析所有 " + 
            (allParetoFronts.size() * allParetoFronts.get(0).size()) + 
            " 次实验的Pareto前沿...\n");
        
        double[] minValues = new double[2];
        double[] maxValues = new double[2];
        Arrays.fill(minValues, Double.POSITIVE_INFINITY);
        Arrays.fill(maxValues, Double.NEGATIVE_INFINITY);
        
        int totalFronts = 0;
        int totalIndividuals = 0;
        
        // 遍历所有实验组
        for (int expGroup = 0; expGroup < allParetoFronts.size(); expGroup++) {
            List<List<MOIndividual>> groupFronts = allParetoFronts.get(expGroup);
            
            // 遍历每组的所有运行
            for (List<MOIndividual> paretoFront : groupFronts) {
                totalFronts++;
                totalIndividuals += paretoFront.size();
                
                // 更新全局最小值和最大值
                for (MOIndividual ind : paretoFront) {
                    minValues[0] = Math.min(minValues[0], ind.objectives[0]);
                    minValues[1] = Math.min(minValues[1], ind.objectives[1]);
                    maxValues[0] = Math.max(maxValues[0], ind.objectives[0]);
                    maxValues[1] = Math.max(maxValues[1], ind.objectives[1]);
                }
            }
        }
        
        System.out.println("统计信息:");
        System.out.println("  总运行次数: " + totalFronts);
        System.out.println("  总Pareto解数: " + totalIndividuals);
        System.out.println("  Cmax范围: [" + String.format("%.2f", minValues[0]) + 
                         ", " + String.format("%.2f", maxValues[0]) + "]");
        System.out.println("  Energy范围: [" + String.format("%.2f", minValues[1]) + 
                         ", " + String.format("%.2f", maxValues[1]) + "]\n");
        
        return new NormalizationInfo(minValues, maxValues);
    }
    
    /**
     * 归一化Pareto前沿
     * 
     * @param front 原始Pareto前沿
     * @param minValues 各目标的最小值
     * @param maxValues 各目标的最大值
     * @return 归一化后的Pareto前沿（目标值在[0,1]区间）
     */
    private static List<MOIndividual> normalizeParetoFront(
            List<MOIndividual> front,
            double[] minValues,
            double[] maxValues) {
        
        List<MOIndividual> normalizedFront = new ArrayList<>();
        
        for (MOIndividual ind : front) {
            // 创建归一化的个体（只归一化目标值）
            MOIndividual normalized = new MOIndividual(ind);
            
            // 归一化各目标值
            for (int i = 0; i < ind.objectives.length; i++) {
                double range = maxValues[i] - minValues[i];
                if (range > 0) {
                    normalized.objectives[i] = (ind.objectives[i] - minValues[i]) / range;
                } else {
                    normalized.objectives[i] = 0.0;  // 范围为0时设为0
                }
            }
            
            normalizedFront.add(normalized);
        }
        
        return normalizedFront;
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
    
    /**
     * 打印欢迎信息
     */
    private static void printWelcome() {
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                ║");
        System.out.println("║          NSGA-II 田口实验 - 参数优化                          ║");
        System.out.println("║                                                                ║");
        System.out.println("║  实验设计: L9(3^4) 正交表                                     ║");
        System.out.println("║  评价指标: 超体积 (Hypervolume, HV)                           ║");
        System.out.println("║  重复次数: 10次                                                ║");
        System.out.println("║                                                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
    }
    
    /**
     * 打印实验设计
     */
    private static void printExperimentDesign() {
        System.out.println("实验设计: L9(3^4)");
        System.out.println();
        System.out.println("因子:");
        System.out.println("  A: 种群规模 (popSize)");
        System.out.println("     水平1=50, 水平2=100, 水平3=150");
        System.out.println("  B: 交叉率 (crossoverRate)");
        System.out.println("     水平1=0.70, 水平2=0.80, 水平3=0.90");
        System.out.println("  C: 变异率 (mutationRate)");
        System.out.println("     水平1=0.10, 水平2=0.20, 水平3=0.30");
        System.out.println("  D: 每代搜索次数L (每个精英的尝试次数)");
        System.out.println("     水平1=5, 水平2=10, 水平3=20");
        System.out.println();
        System.out.println("注意: 局部搜索每代都执行 (interval=1)");
        System.out.println("评价指标: 超体积 (HV) - 越大越好");
        System.out.println("每组参数运行 " + REPEAT_TIMES + " 次，取平均值");
        System.out.println();
    }
    
    /**
     * 打印最终总结
     */
    private static void printFinalSummary(double[] avgHV) {
        System.out.println("\n════════════════════════════════════════════════════════════════");
        System.out.println("                   田口实验完成总结");
        System.out.println("════════════════════════════════════════════════════════════════");
        System.out.println();
        
        // 找出最优实验
        int bestExp = 0;
        double bestHV = avgHV[0];
        for (int i = 1; i < avgHV.length; i++) {
            if (avgHV[i] > bestHV) {
                bestHV = avgHV[i];
                bestExp = i;
            }
        }
        
        System.out.println("最优实验组: 实验 " + (bestExp + 1));
        System.out.println("最优平均HV: " + String.format("%.6f", bestHV));
        System.out.println();
        System.out.println("所有实验结果已保存到: " + OUTPUT_DIR);
        System.out.println();
        System.out.println("请查看详细报告了解:");
        System.out.println("  - 因子效应分析");
        System.out.println("  - 推荐参数配置");
        System.out.println("  - 实验结果排名");
        System.out.println("  - 详细数据记录");
        System.out.println();
        System.out.println("════════════════════════════════════════════════════════════════\n");
    }
    
    // 参数值转换方法
    private static int getPopSizeValue(int level) {
        return new int[]{50, 100, 150}[level];
    }
    
    private static double getCrossoverRateValue(int level) {
        return new double[]{0.70, 0.80, 0.90}[level];
    }
    
    private static double getMutationRateValue(int level) {
        return new double[]{0.10, 0.20, 0.30}[level];
    }
    
    private static int getLocalSearchLValue(int level) {
        return new int[]{5, 10, 20}[level];
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 实验原始数据类（阶段1）
     */
    private static class ExperimentRawData {
        List<List<MOIndividual>> paretoFronts;  // 所有运行的Pareto前沿
        double[] runTimes;                       // 运行时间
        double avgParetoSize;                    // 平均Pareto前沿规模
        double avgRunTime;                       // 平均运行时间
    }
    
    /**
     * 归一化信息类
     */
    private static class NormalizationInfo {
        double[] minValues;  // 各目标的最小值
        double[] maxValues;  // 各目标的最大值
        
        NormalizationInfo(double[] minValues, double[] maxValues) {
            this.minValues = minValues;
            this.maxValues = maxValues;
        }
    }
    
    /**
     * 单个实验任务类（用于多线程）
     */
    private static class ExperimentTask implements Callable<ExperimentTask.Result> {
        
        private final Problem problem;
        private final List<MOEvaluator.ObjectiveFunction> objectives;
        private final String instanceName;
        private final int experimentNo;
        private final int runNo;
        private final int[] config;
        private final AtomicInteger completedTasks;
        private final int totalTasks;
        
        public ExperimentTask(Problem problem,
                            List<MOEvaluator.ObjectiveFunction> objectives,
                            String instanceName,
                            int experimentNo,
                            int runNo,
                            int[] config,
                            AtomicInteger completedTasks,
                            int totalTasks) {
            this.problem = problem;
            this.objectives = objectives;
            this.instanceName = instanceName;
            this.experimentNo = experimentNo;
            this.runNo = runNo;
            this.config = config;
            this.completedTasks = completedTasks;
            this.totalTasks = totalTasks;
        }
        
        @Override
        public Result call() {
            long startTime = System.currentTimeMillis();
            
            try {
                // 创建NSGA-II实例
                NSGAII nsgaii = new NSGAII(problem, objectives);
                
                // 设置参数
                nsgaii.setPopulationSize(config[0]);
                nsgaii.setCrossoverRate(config[1] / 100.0);
                nsgaii.setMutationRate(config[2] / 100.0);
                nsgaii.setMaxGenerations(MAX_GENERATIONS);
                nsgaii.setMaxRunTimeMinutes(MAX_RUN_TIME);
                
                // 局部搜索
                if (ENABLE_LOCAL_SEARCH) {
                    nsgaii.enableLocalSearch(true, 1);
                    nsgaii.setLocalSearchParameters(
                        config[3], 0.10, 0.05, 0.02, 0.005, 0.01
                    );
                }
                
                // 设置随机种子
                nsgaii.setSeed(System.currentTimeMillis() + runNo * 1000);
                
                // 运行算法
                List<MOIndividual> paretoFront = nsgaii.solve();
                long endTime = System.currentTimeMillis();
                
                double runTime = (endTime - startTime) / 1000.0;
                
                // 更新进度
                int completed = completedTasks.incrementAndGet();
                synchronized (System.out) {
                    System.out.printf("\r进度: [%3d/%3d] 实验组%d-运行%d 完成 (Pareto规模=%d, 耗时=%.1f秒)          ",
                                    completed, totalTasks, experimentNo, runNo, 
                                    paretoFront.size(), runTime);
                }
                
                // 返回结果
                String key = "exp" + experimentNo + "_run" + runNo;
                return new Result(key, paretoFront, runTime);
                
            } catch (Exception e) {
                System.err.println("\n❌ 实验组" + experimentNo + "-运行" + runNo + "失败: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
        
        /**
         * 结果类
         */
        static class Result {
            String key;
            List<MOIndividual> paretoFront;
            double runTime;
            
            Result(String key, List<MOIndividual> paretoFront, double runTime) {
                this.key = key;
                this.paretoFront = paretoFront;
                this.runTime = runTime;
            }
        }
    }
}
