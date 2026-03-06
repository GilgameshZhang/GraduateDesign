import AlgorithmFrame.machineChoice.ga.BatchGa;
import AlgorithmFrame.alns.*;
import ProblemFrame.Input;
import ProblemFrame.Result;
import util.Chapter1Visualizer;
import util.ReadDataUtil;
import util.ExperimentResultWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;

/**
 * 多算法对比实验（支持多线程并行）
 * 包含算法：GA-TS、GA-GA、GA-ACO、ALNS
 */
public class MultiAlgorithmComparison {
    
    /**
     * 算法类型枚举
     */
    enum AlgorithmType {
        GA_TS("GA-TS", "外层GA+内层禁忌搜索"),
        GA_GA("GA-GA", "外层GA+内层遗传算法"),
        GA_ACO("GA-ACO", "外层GA+内层蚁群算法"),
        ALNS("ALNS", "自适应大邻域搜索");
        
        final String name;
        final String description;
        
        AlgorithmType(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }
    
    /**
     * 实验结果类
     */
    static class ExperimentResult {
        AlgorithmType algorithm;
        String instanceName;
        int runId;
        double cmax;
        double avgUtilization;
        long timeMs;
        Result gaResult;      // GA算法的结果（用于可视化）
        Input input;          // 输入数据（用于可视化）
        
        // ALNS专用
        ALNSSolution alnsResult;
        ALNS alnsInstance;
    }
    
    /**
     * 统计信息类
     */
    static class Statistics {
        AlgorithmType algorithm;
        String instanceName;
        double avgCmax;
        double minCmax;
        double maxCmax;
        double stdDevCmax;
        double avgUtilization;
        double avgTimeMs;
        ExperimentResult bestResult;
    }
    
    /**
     * 运行单次实验的任务
     */
    static class ExperimentTask implements Callable<ExperimentResult> {
        private final AlgorithmType algorithm;
        private final String machinePath;
        private final String itemPath;
        private final String instanceName;
        private final int runId;
        private final long seed;
        
        public ExperimentTask(AlgorithmType algorithm, String machinePath, String itemPath,
                            String instanceName, int runId) {
            this.algorithm = algorithm;
            this.machinePath = machinePath;
            this.itemPath = itemPath;
            this.instanceName = instanceName;
            this.runId = runId;
            this.seed = 12345L + runId;
        }
        
        @Override
        public ExperimentResult call() throws Exception {
            System.out.println(String.format("[%s] [%s] 运行 %d 开始...", 
                algorithm.name, instanceName, runId));
            
            try {
                if (algorithm == AlgorithmType.ALNS) {
                    return runALNS();
                } else {
                    return runGA(algorithm);
                }
            } catch (Exception e) {
                System.err.println(String.format("[%s] [%s] 运行 %d 失败: %s", 
                    algorithm.name, instanceName, runId, e.getMessage()));
                e.printStackTrace();
                return null;
            }
        }
        
        /**
         * 运行GA类算法（GA-TS、GA-GA、GA-ACO）
         */
        private ExperimentResult runGA(AlgorithmType algType) throws Exception {
            Input input = ReadDataUtil.readData(machinePath, itemPath, true);
            
            // 确定内层优化方法
            String method;
            switch (algType) {
                case GA_TS:
                    method = "TabuSearch";
                    break;
                case GA_GA:
                    method = "GA";
                    break;
                case GA_ACO:
                    method = "ACO";
                    break;
                default:
                    method = "TabuSearch";
            }
            
            // 设置5分钟时间限制
            long timeLimitMs = 1 * 30 * 1000;  // 5分钟
            
            BatchGa batchGa = new BatchGa(
                1000000000,  // MAX_GEN - 设置一个很大的值，实际由时间控制
                100,    // popSize
                5,      // variationExchangeCount
                2,      // cloneNumOfBestIndividual
                0.2,    // mutationRate
                0.8,    // crossoverRate
                input,
                true,   // isRotateEnable
                method,
                100,    // decodeMaxGen
                10,     // decodeTabuSize
                30,     // decodeMaxN
                timeLimitMs  // 时间限制：5分钟
            );
            
            long startTime = System.currentTimeMillis();
            Result result = batchGa.solve();
            long endTime = System.currentTimeMillis();
            
            ExperimentResult expResult = new ExperimentResult();
            expResult.algorithm = algType;
            expResult.instanceName = instanceName;
            expResult.runId = runId;
            expResult.cmax = batchGa.bestGenome.cMax;
            expResult.avgUtilization = calculateAvgUtilization(batchGa);
            expResult.timeMs = endTime - startTime;
            expResult.gaResult = result;
            expResult.input = input;
            
            System.out.println(String.format("[%s] [%s] 运行 %d 完成: Cmax=%.4f, 用时=%dms", 
                algType.name, instanceName, runId, expResult.cmax, expResult.timeMs));
            
            // 为每次运行生成完整的实验报告和图表
            try {
                ExperimentResultWriter writer = new ExperimentResultWriter(
                    algType.name, runId, instanceName
                );
                
                writer.writeExperimentInfo(
                    algType.name + " - " + algType.description, runId, instanceName,
                    10000, 100, 0.2, 0.8
                );
                
                writer.writeFinalResults(
                    batchGa.bestGenome.cMax,
                    endTime - startTime,
                    batchGa.t,
                    result.getIreatorList(),
                    batchGa.bestGenome,
                    batchGa.machines
                );
                
                writer.writeMachineLoads(batchGa.bestGenome);
                writer.close();
                
                System.out.println(String.format("[%s] ✅ 实验报告和图表已生成", algType.name));
            } catch (Exception e) {
                System.err.println(String.format("[%s] ⚠️ 生成报告失败: %s", algType.name, e.getMessage()));
            }
            
            return expResult;
        }
        
        /**
         * 运行ALNS算法
         */
        private ExperimentResult runALNS() throws Exception {
            ReadDataUtil readDataUtil = new ReadDataUtil();
            String[] pathList = {itemPath, machinePath};
            Input input = readDataUtil.getInput(pathList);
            
            ALNSAdapter.ALNSData alnsData = ALNSAdapter.convertInput(input);
            
            ALNSParameters params = new ALNSParameters();
            params.maxIterations = 1000000000;  // 设置一个很大的值，实际由时间控制
            params.timeLimitMs = 1 * 30 * 1000; // 5分钟时间限制
            
            ALNS alns = new ALNS(alnsData.jobs, alnsData.machines, params, seed);
            
            long startTime = System.currentTimeMillis();
            ALNSSolution solution = alns.solve();
            long endTime = System.currentTimeMillis();
            
            ExperimentResult expResult = new ExperimentResult();
            expResult.algorithm = AlgorithmType.ALNS;
            expResult.instanceName = instanceName;
            expResult.runId = runId;
            expResult.cmax = solution.cmax;
            expResult.avgUtilization = 0.0; // ALNS不计算利用率
            expResult.timeMs = endTime - startTime;
            expResult.alnsResult = solution;
            expResult.alnsInstance = alns;
            expResult.input = input;
            
            System.out.println(String.format("[%s] [%s] 运行 %d 完成: Cmax=%.4f, 用时=%dms", 
                "ALNS", instanceName, runId, expResult.cmax, expResult.timeMs));
            
            return expResult;
        }
        
        /**
         * 计算平均利用率
         */
        private double calculateAvgUtilization(BatchGa batchGa) {
            if (batchGa.bestGenome == null || batchGa.bestGenome.solutions == null) {
                return 0.0;
            }
            
            double totalRate = 0.0;
            int count = 0;
            
            for (int i = 0; i < batchGa.bestGenome.solutions.size(); i++) {
                if (batchGa.bestGenome.solutions.get(i).solutions != null) {
                    for (int j = 0; j < batchGa.bestGenome.solutions.get(i).solutions.size(); j++) {
                        totalRate += batchGa.bestGenome.solutions.get(i).solutions.get(j).rate;
                        count++;
                    }
                }
            }
            
            return count > 0 ? totalRate / count : 0.0;
        }
    }
    
    /**
     * 运行多算法对比实验（多线程版本）
     */
    public static void runMultiAlgorithmComparison(
            String machinePath, 
            String itemPath,
            String instanceName,
            int numRuns,
            boolean enableVisualization) {
        
        System.out.println("========================================");
        System.out.println("多算法对比实验（多线程并行）");
        System.out.println("========================================");
        System.out.println("算例: " + instanceName);
        System.out.println("机器文件: " + machinePath);
        System.out.println("作业文件: " + itemPath);
        System.out.println("每个算法运行次数: " + numRuns);
        System.out.println("可视化: " + (enableVisualization ? "开启" : "关闭"));
        System.out.println("========================================\n");
        
        // 获取可用处理器数量
        int numThreads = Runtime.getRuntime().availableProcessors();
        System.out.println("使用线程数: " + numThreads);
        System.out.println("总任务数: " + (AlgorithmType.values().length * numRuns) + "\n");
        
        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        // 创建所有任务
        List<Future<ExperimentResult>> futures = new ArrayList<>();
        Map<AlgorithmType, List<ExperimentResult>> allResults = new HashMap<>();
        
        for (AlgorithmType algorithm : AlgorithmType.values()) {
            allResults.put(algorithm, Collections.synchronizedList(new ArrayList<>()));
            
            for (int run = 1; run <= numRuns; run++) {
                ExperimentTask task = new ExperimentTask(
                    algorithm, machinePath, itemPath, instanceName, run
                );
                futures.add(executor.submit(task));
            }
        }
        
        // 等待所有任务完成
        System.out.println("等待所有任务完成...\n");
        for (Future<ExperimentResult> future : futures) {
            try {
                ExperimentResult result = future.get();
                if (result != null) {
                    allResults.get(result.algorithm).add(result);
                }
            } catch (Exception e) {
                System.err.println("任务执行失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // 关闭线程池
        executor.shutdown();
        
        System.out.println("\n所有算法运行完成！\n");
        
        // 统计和可视化
        Map<AlgorithmType, Statistics> statsMap = new HashMap<>();
        
        for (AlgorithmType algorithm : AlgorithmType.values()) {
            List<ExperimentResult> results = allResults.get(algorithm);
            if (!results.isEmpty()) {
                Statistics stats = calculateStatistics(algorithm, instanceName, results);
                statsMap.put(algorithm, stats);
                printStatistics(stats);
                
                // 生成可视化（为最优运行）
                if (enableVisualization && stats.bestResult != null) {
                    generateVisualization(stats.bestResult, instanceName);
                }
            }
        }
        
        // 保存综合结果
        saveComparisonResults(allResults, statsMap, instanceName, machinePath, itemPath);
        
        System.out.println("\n========================================");
        System.out.println("多算法对比实验完成！");
        System.out.println("结果已保存到: chapter-1/src/main/output/multi_algorithm_comparison/");
        System.out.println("========================================");
    }
    
    /**
     * 计算统计信息
     */
    private static Statistics calculateStatistics(
            AlgorithmType algorithm, 
            String instanceName,
            List<ExperimentResult> results) {
        
        Statistics stats = new Statistics();
        stats.algorithm = algorithm;
        stats.instanceName = instanceName;
        
        double sumCmax = 0.0;
        double sumUtil = 0.0;
        double sumTime = 0.0;
        double minCmax = Double.MAX_VALUE;
        double maxCmax = -Double.MAX_VALUE;
        
        for (ExperimentResult result : results) {
            sumCmax += result.cmax;
            sumUtil += result.avgUtilization;
            sumTime += result.timeMs;
            
            if (result.cmax < minCmax) {
                minCmax = result.cmax;
                stats.bestResult = result;
            }
            if (result.cmax > maxCmax) {
                maxCmax = result.cmax;
            }
        }
        
        int n = results.size();
        stats.avgCmax = sumCmax / n;
        stats.avgUtilization = sumUtil / n;
        stats.avgTimeMs = sumTime / n;
        stats.minCmax = minCmax;
        stats.maxCmax = maxCmax;
        
        // 计算标准差
        double sumSquaredDiff = 0.0;
        for (ExperimentResult result : results) {
            double diff = result.cmax - stats.avgCmax;
            sumSquaredDiff += diff * diff;
        }
        stats.stdDevCmax = Math.sqrt(sumSquaredDiff / n);
        
        return stats;
    }
    
    /**
     * 打印统计结果
     */
    private static void printStatistics(Statistics stats) {
        System.out.println("========================================");
        System.out.println(String.format("%s 算法统计结果 [%s]", 
            stats.algorithm.name, stats.instanceName));
        System.out.println("========================================");
        System.out.println(String.format("平均Cmax:      %.4f", stats.avgCmax));
        System.out.println(String.format("最小Cmax:      %.4f (运行 %d)", 
            stats.minCmax, stats.bestResult.runId));
        System.out.println(String.format("最大Cmax:      %.4f", stats.maxCmax));
        System.out.println(String.format("标准差:        %.4f", stats.stdDevCmax));
        if (stats.algorithm != AlgorithmType.ALNS) {
            System.out.println(String.format("平均利用率:    %.2f%%", stats.avgUtilization * 100));
        }
        System.out.println(String.format("平均时间:      %.2f ms", stats.avgTimeMs));
        System.out.println("========================================\n");
    }
    
    /**
     * 生成可视化
     */
    private static void generateVisualization(ExperimentResult result, String instanceName) {
        try {
            String outputDir = String.format(
                "chapter-1/src/main/output/multi_algorithm_comparison/%s/%s/run_%d",
                instanceName, result.algorithm.name, result.runId
            );
            
            System.out.println(String.format("生成 [%s] 最优运行的可视化图表...", 
                result.algorithm.name));
            
            if (result.algorithm == AlgorithmType.ALNS) {
                // ALNS专用可视化
                AlgorithmFrame.alns.visualization.ALNSVisualizer visualizer = 
                    new AlgorithmFrame.alns.visualization.ALNSVisualizer(
                        result.alnsResult,
                        result.alnsInstance.getJobs(),
                        result.alnsInstance.getMachines(),
                        result.alnsInstance.getIterationHistory()
                    );
                visualizer.generateAllCharts(outputDir);
            } else {
                // GA类算法通用可视化
                Chapter1Visualizer.generateAllCharts(
                    result.gaResult.getSolutionList(),
                    result.input.machineList.toArray(new ProblemFrame.Machine[0]),
                    result.gaResult.getIreatorList(),
                    result.algorithm.name + " 算法",
                    outputDir
                );
            }
            
            System.out.println("可视化图表已保存到: " + outputDir + "\n");
            
        } catch (Exception e) {
            System.err.println("生成可视化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 保存综合对比结果
     */
    private static void saveComparisonResults(
            Map<AlgorithmType, List<ExperimentResult>> allResults,
            Map<AlgorithmType, Statistics> statsMap,
            String instanceName,
            String machinePath,
            String itemPath) {
        
        String baseDir = "chapter-1/src/main/output/multi_algorithm_comparison/" + instanceName;
        new java.io.File(baseDir).mkdirs();
        
        // 1. 保存详细结果
        String detailPath = baseDir + "/detailed_results.csv";
        try (PrintWriter writer = new PrintWriter(new FileWriter(detailPath))) {
            writer.println("Algorithm,RunID,Cmax,AvgUtilization,TimeMs,MachinePath,ItemPath");
            
            for (AlgorithmType algorithm : AlgorithmType.values()) {
                List<ExperimentResult> results = allResults.get(algorithm);
                for (ExperimentResult result : results) {
                    writer.println(String.format("%s,%d,%.4f,%.4f,%d,%s,%s",
                        algorithm.name,
                        result.runId,
                        result.cmax,
                        result.avgUtilization,
                        result.timeMs,
                        machinePath,
                        itemPath
                    ));
                }
            }
            
            System.out.println("详细结果已保存: " + detailPath);
        } catch (IOException e) {
            System.err.println("保存详细结果失败: " + e.getMessage());
        }
        
        // 2. 保存统计摘要
        String summaryPath = baseDir + "/summary_statistics.csv";
        try (PrintWriter writer = new PrintWriter(new FileWriter(summaryPath))) {
            writer.println("Algorithm,AvgCmax,MinCmax,MaxCmax,StdDev,AvgUtilization,AvgTimeMs");
            
            for (AlgorithmType algorithm : AlgorithmType.values()) {
                Statistics stats = statsMap.get(algorithm);
                if (stats != null) {
                    writer.println(String.format("%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f",
                        algorithm.name,
                        stats.avgCmax,
                        stats.minCmax,
                        stats.maxCmax,
                        stats.stdDevCmax,
                        stats.avgUtilization,
                        stats.avgTimeMs
                    ));
                }
            }
            
            System.out.println("统计摘要已保存: " + summaryPath);
        } catch (IOException e) {
            System.err.println("保存统计摘要失败: " + e.getMessage());
        }
        
        // 3. 保存对比报告
        String reportPath = baseDir + "/comparison_report.txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(reportPath))) {
            writer.println("========================================");
            writer.println("多算法对比实验报告");
            writer.println("========================================");
            writer.println("算例: " + instanceName);
            writer.println("机器文件: " + machinePath);
            writer.println("作业文件: " + itemPath);
            writer.println();
            
            // 找出最优算法
            AlgorithmType bestAlg = null;
            double bestAvgCmax = Double.MAX_VALUE;
            
            for (AlgorithmType algorithm : AlgorithmType.values()) {
                Statistics stats = statsMap.get(algorithm);
                if (stats != null && stats.avgCmax < bestAvgCmax) {
                    bestAvgCmax = stats.avgCmax;
                    bestAlg = algorithm;
                }
            }
            
            writer.println("========== 算法性能对比 ==========");
            writer.println();
            writer.println(String.format("%-15s %-12s %-12s %-12s %-12s", 
                "算法", "平均Cmax", "最优Cmax", "标准差", "平均时间(ms)"));
            // Java 8兼容的字符串重复
            StringBuilder separator = new StringBuilder();
            for (int i = 0; i < 70; i++) {
                separator.append("-");
            }
            writer.println(separator.toString());
            
            for (AlgorithmType algorithm : AlgorithmType.values()) {
                Statistics stats = statsMap.get(algorithm);
                if (stats != null) {
                    String marker = (algorithm == bestAlg) ? " *最优*" : "";
                    writer.println(String.format("%-15s %-12.4f %-12.4f %-12.4f %-12.2f%s", 
                        algorithm.name,
                        stats.avgCmax,
                        stats.minCmax,
                        stats.stdDevCmax,
                        stats.avgTimeMs,
                        marker
                    ));
                }
            }
            
            writer.println();
            writer.println("========== 结论 ==========");
            writer.println("最优算法: " + (bestAlg != null ? bestAlg.name : "N/A"));
            writer.println("最优平均Cmax: " + String.format("%.4f", bestAvgCmax));
            
            System.out.println("对比报告已保存: " + reportPath);
        } catch (IOException e) {
            System.err.println("保存对比报告失败: " + e.getMessage());
        }
    }
    
    /**
     * 主函数 - 自动生成梯队式算例
     * 
     * 实验设计：
     * - 5个规模梯队：小(2M10J)、中小(3M20J)、中(3M40J)、中大(4M60J)、大(5M80J)
     * - 每个规模4个案例，共20个算例
     * - 每个算例：4算法 × 3次 × 5分钟
     * 
     * 预计耗时：多线程约5-7小时，单线程约20小时
     */
    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("多算法对比实验 - 梯队式完整实验（20个算例）");
        System.out.println("================================================================================");
        System.out.println();
        System.out.println("实验配置：");
        System.out.println("  - 算法：GA-TS, GA-GA, GA-ACO, ALNS");
        System.out.println("  - 规模：5个梯队");
        System.out.println("  - 算例：每个规模4个案例，共20个");
        System.out.println("  - 重复：每个算例每个算法运行3次");
        System.out.println("  - 时间：每次运行5分钟");
        System.out.println("  - 预计总时间：多线程约5-7小时");
        System.out.println();
        System.out.println("================================================================================");
        System.out.println();
        
        int totalInstances = 0;
        long startTime = System.currentTimeMillis();
        
        // 规模1：小规模 - 2台机器 × 10个工件（4个案例）
        System.out.println("\n【规模1/5】小规模：2台机器 × 10个工件（4个案例）");
        for (int i = 1; i <= 4; i++) {
            totalInstances++;
            System.out.println(String.format("\n进度 [%d/20] 开始算例：Small-2M10J-%02d", totalInstances, i));
            runMultiAlgorithmComparison(
                "chapter-1/src/main/resources/Machine/machine_2",
                String.format("chapter-1/src/main/resources/PrintItem/printItem_10/printItem_10_%02d", i),
                String.format("2M10J-%02d", i),
                10,
                true
            );
        }
        
        // 规模2：中小规模 - 3台机器 × 20个工件（4个案例）
        System.out.println("\n【规模2/5】中小规模：2台机器 × 20个工件（4个案例）");
        for (int i = 1; i <= 4; i++) {
            totalInstances++;
            System.out.println(String.format("\n进度 [%d/20] 开始算例：MediumSmall-3M20J-%02d", totalInstances, i));
            runMultiAlgorithmComparison(
                "chapter-1/src/main/resources/Machine/machine_2",
                String.format("chapter-1/src/main/resources/PrintItem/printItem_20/printItem_20_%02d", i),
                String.format("2M20J-%02d", i),
                10,
                true
            );
        }
        
        // 规模3：中等规模 - 3台机器 × 40个工件（4个案例）
        System.out.println("\n【规模3/5】中等规模：2台机器 × 40个工件（4个案例）");
        for (int i = 1; i <= 4; i++) {
            totalInstances++;
            System.out.println(String.format("\n进度 [%d/20] 开始算例：Medium-3M40J-%02d", totalInstances, i));
            runMultiAlgorithmComparison(
                "chapter-1/src/main/resources/Machine/machine_2",
                String.format("chapter-1/src/main/resources/PrintItem/printItem_40/printItem_40_%02d", i),
                String.format("2M40J-%02d", i),
                10,
                true
            );
        }
        
        // 规模4：中大规模 - 4台机器 × 60个工件（4个案例）
        System.out.println("\n【规模4/5】中大规模：4台机器 × 60个工件（4个案例）");
        for (int i = 1; i <= 4; i++) {
            totalInstances++;
            System.out.println(String.format("\n进度 [%d/20] 开始算例：MediumLarge-4M60J-%02d", totalInstances, i));
            runMultiAlgorithmComparison(
                "chapter-1/src/main/resources/Machine/machine_3",
                String.format("chapter-1/src/main/resources/PrintItem/printItem_60/printItem_60_%02d", i),
                String.format("3M60J-%02d", i),
                10,
                true
            );
        }
        
        // 规模5：大规模 - 5台机器 × 80个工件（4个案例）
        System.out.println("\n【规模5/5】大规模：3台机器 × 80个工件（4个案例）");
        for (int i = 1; i <= 4; i++) {
            totalInstances++;
            System.out.println(String.format("\n进度 [%d/20] 开始算例：Large-5M80J-%02d", totalInstances, i));
            runMultiAlgorithmComparison(
                "chapter-1/src/main/resources/Machine/machine_5",
                String.format("chapter-1/src/main/resources/PrintItem/printItem_80/printItem_80_%02d", i),
                String.format("4M80J-%02d", i),
                10,
                true
            );
        }
        
        long endTime = System.currentTimeMillis();
        double totalHours = (endTime - startTime) / 3600000.0;
        
        System.out.println("\n================================================================================");
        System.out.println("所有实验完成！");
        System.out.println("  - 完成算例数：" + totalInstances);
        System.out.println("  - 总耗时：" + String.format("%.2f小时", totalHours));
        System.out.println("  - 结果目录：multi_algorithm_comparison_results/");
        System.out.println("================================================================================");
    }
}
