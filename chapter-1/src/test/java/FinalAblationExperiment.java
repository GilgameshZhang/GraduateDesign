import AlgorithmFrame.machineChoice.ga.BatchGa;
import AlgorithmFrame.machineChoice.ga.BatchGaAblation;
import AlgorithmFrame.machineChoice.ga.BatchGaSimpleMutation;
import ProblemFrame.Input;
import ProblemFrame.Result;
import util.ReadDataUtil;
import util.ExperimentResultWriter;
import util.ChartGenerator;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 完整消融实验运行器
 * 消融4个关键组件：
 * 1. 初始化策略（50%启发式 vs 0%随机）
 * 2. 变异算子（LocalSearch vs Simple）
 * 3. 禁忌搜索（TabuSearch vs NoSearch）
 * 4. 评分策略（Complex vs Simple）
 */
public class FinalAblationExperiment {
    
    /**
     * 消融配置类
     */
    static class AblationConfig {
        String configId;
        String configName;
        double heuristicInitRatio;      // 启发式初始化比例 (0.5=50%, 0.0=0%)
        boolean useLocalSearchMutation; // 是否使用LocalSearch变异
        boolean useTabuSearch;          // 是否使用禁忌搜索
        boolean useComplexScore;        // 是否使用复杂评分策略
        String description;
        
        public AblationConfig(String id, String name, double initRatio, 
                            boolean localMut, boolean tabu, boolean complexScore, String desc) {
            this.configId = id;
            this.configName = name;
            this.heuristicInitRatio = initRatio;
            this.useLocalSearchMutation = localMut;
            this.useTabuSearch = tabu;
            this.useComplexScore = complexScore;
            this.description = desc;
        }
    }
    
    /**
     * 实验结果类
     */
    static class ExperimentResult {
        String configId;
        String configName;
        String instanceName;
        int runId;
        double cmax;
        double avgUtilization;
        long timeMs;
        List<Double> iterationHistory;
    }
    
    /**
     * 统计信息类
     */
    static class Statistics {
        String configId;
        String configName;
        String instanceName;
        double avgCmax;
        double minCmax;
        double maxCmax;
        double stdDevCmax;
        double avgUtilization;
        double avgTimeMs;
        double rpd;
        ExperimentResult bestResult;
    }
    
    /**
     * 获取所有消融实验配置
     */
    private static List<AblationConfig> getAblationConfigs() {
        List<AblationConfig> configs = new ArrayList<AblationConfig>();
        
        // V0-Full: 完整算法（基线）
        configs.add(new AblationConfig(
            "V0-Full", "完整算法",
            0.5, true, true, true,
            "50%启发式+LocalSearch变异+禁忌搜索+复杂评分"
        ));
        
        // V1-RandomInit: 无启发式初始化（全随机）
        configs.add(new AblationConfig(
            "V1-RandomInit", "随机初始化",
            0.0, true, true, true,
            "0%启发式(全随机)+LocalSearch变异+禁忌搜索+复杂评分"
        ));
        
        // V2-SimpleMutation: 简化变异算子
        configs.add(new AblationConfig(
            "V2-SimpleMutation", "简化变异",
            0.5, false, true, true,
            "50%启发式+简单变异+禁忌搜索+复杂评分"
        ));
        
        // V3-NoTabu: 无禁忌搜索
        configs.add(new AblationConfig(
            "V3-NoTabu", "无禁忌搜索",
            0.5, true, false, true,
            "50%启发式+LocalSearch变异+无搜索+复杂评分"
        ));
        
        // V4-SimpleScore: 简化评分策略
        configs.add(new AblationConfig(
            "V4-SimpleScore", "简化评分",
            0.5, true, true, false,
            "50%启发式+LocalSearch变异+禁忌搜索+简化评分"
        ));
        
        // V5-Minimal: 最小配置（所有组件都移除）
        configs.add(new AblationConfig(
            "V5-Minimal", "最小配置",
            0.0, false, false, false,
            "全随机初始化+简单变异+无搜索+简化评分"
        ));
        
        return configs;
    }
    
    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("                         完整消融实验");
        System.out.println("================================================================================");
        System.out.println();
        System.out.println("消融4个关键组件：");
        System.out.println("  1. 初始化策略 - 50%启发式 vs 0%随机");
        System.out.println("  2. 变异算子 - LocalSearch(负载平衡) vs Simple(随机)");
        System.out.println("  3. 禁忌搜索 - TabuSearch vs NoSearch");
        System.out.println("  4. 评分策略 - Complex(多指标) vs Simple(单指标)");
        System.out.println();
        System.out.println("实验配置：");
        System.out.println("  - 测试算例：5个不同规模");
        System.out.println("  - 配置数量：6个（V0-Full基线 + 5个消融版本）");
        System.out.println("  - 每配置运行次数：10次");
        System.out.println("  - 时间限制：每次5分钟");
        System.out.println("  - 线程数：最多8个线程并行");
        System.out.println("  - 预计总耗时：约3-5小时（多线程加速）");
        System.out.println("================================================================================");
        System.out.println();
        
        // 测试算例
        String[][] instances = {
            {"chapter-1/src/main/resources/Machine/machine_2", 
             "chapter-1/src/main/resources/PrintItem/printItem_10/printItem_10_01", "2M10J"},
            {"chapter-1/src/main/resources/Machine/machine_2", 
             "chapter-1/src/main/resources/PrintItem/printItem_20/printItem_20_01", "2M20J"},
            {"chapter-1/src/main/resources/Machine/machine_2", 
             "chapter-1/src/main/resources/PrintItem/printItem_40/printItem_40_01", "2M40J"},
            {"chapter-1/src/main/resources/Machine/machine_3", 
             "chapter-1/src/main/resources/PrintItem/printItem_60/printItem_60_01", "3M60J"},
            {"chapter-1/src/main/resources/Machine/machine_5", 
             "chapter-1/src/main/resources/PrintItem/printItem_80/printItem_80_01", "5M80J"}
        };
        
        long startTime = System.currentTimeMillis();
        
        List<AblationConfig> configs = getAblationConfigs();
        
        // 存储所有结果
        Map<String, Map<String, List<ExperimentResult>>> allResults = 
            new HashMap<String, Map<String, List<ExperimentResult>>>();
        Map<String, Map<String, Statistics>> allStats = 
            new HashMap<String, Map<String, Statistics>>();
        
        // 对每个算例运行实验
        for (String[] instance : instances) {
            String machinePath = instance[0];
            String itemPath = instance[1];
            String instanceName = instance[2];
            
            System.out.println("\n" + repeat("=", 80));
            System.out.println("开始算例：" + instanceName);
            System.out.println(repeat("=", 80));
            
            Map<String, List<ExperimentResult>> instanceResults = 
                new HashMap<String, List<ExperimentResult>>();
            for (AblationConfig config : configs) {
                instanceResults.put(config.configId, Collections.synchronizedList(new ArrayList<ExperimentResult>()));
            }
            
            // 使用线程池并行运行实验
            int numRuns = 10;
            // 根据内存情况调整线程数（避免OOM）
            // 每个GA实例约占用500MB-1GB内存，限制并发数为2-3个
            int numThreads = Math.min(Runtime.getRuntime().availableProcessors(), 3); // 最多3个线程
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            
            System.out.println(String.format("使用 %d 个线程并行运行实验（内存优化）...", numThreads));
            System.out.println(String.format("可用内存: %.2f GB / 最大内存: %.2f GB", 
                Runtime.getRuntime().freeMemory() / 1024.0 / 1024.0 / 1024.0,
                Runtime.getRuntime().maxMemory() / 1024.0 / 1024.0 / 1024.0));
            
            // 创建所有任务
            List<Callable<ExperimentResult>> tasks = new ArrayList<>();
            AtomicInteger completedTasks = new AtomicInteger(0);
            int totalTasks = numRuns * configs.size();
            
            for (int run = 1; run <= numRuns; run++) {
                for (AblationConfig config : configs) {
                    final int currentRun = run;
                    final AblationConfig currentConfig = config;
                    
                    tasks.add(new Callable<ExperimentResult>() {
                        @Override
                        public ExperimentResult call() throws Exception {
                            ExperimentResult result = runSingleExperiment(
                                machinePath, itemPath, instanceName, currentConfig, currentRun
                            );
                            
                            int completed = completedTasks.incrementAndGet();
                            synchronized (System.out) {
                                System.out.println(String.format(
                                    "[%s] 进度 %d/%d (%.1f%%) | 运行 %d | %s | Cmax=%.4f | 用时=%dms",
                                    instanceName, completed, totalTasks, 
                                    (completed * 100.0 / totalTasks),
                                    currentRun, currentConfig.configName,
                                    result.cmax, result.timeMs
                                ));
                            }
                            
                            return result;
                        }
                    });
                }
            }
            
            // 执行所有任务
            try {
                List<Future<ExperimentResult>> futures = executor.invokeAll(tasks);
                
                // 收集结果
                for (Future<ExperimentResult> future : futures) {
                    try {
                        ExperimentResult result = future.get();
                        if (result != null) {
                            instanceResults.get(result.configId).add(result);
                        }
                    } catch (ExecutionException e) {
                        System.err.println("实验任务执行失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } catch (InterruptedException e) {
                System.err.println("实验被中断: " + e.getMessage());
                e.printStackTrace();
            } finally {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                }
            }
            
            // 计算统计信息
            Map<String, Statistics> instanceStats = new HashMap<String, Statistics>();
            for (AblationConfig config : configs) {
                Statistics stats = calculateStatistics(
                    config, instanceName, instanceResults.get(config.configId)
                );
                instanceStats.put(config.configId, stats);
            }
            
            // 计算RPD（相对于V0-Full）
            Statistics fullStats = instanceStats.get("V0-Full");
            if (fullStats != null) {
                for (AblationConfig config : configs) {
                    Statistics stats = instanceStats.get(config.configId);
                    if (stats != null) {
                        if (config.configId.equals("V0-Full")) {
                            stats.rpd = 0.0;
                        } else {
                            stats.rpd = ((stats.avgCmax - fullStats.avgCmax) / fullStats.avgCmax) * 100;
                        }
                    }
                }
            }
            
            // 打印统计结果
            for (AblationConfig config : configs) {
                printStatistics(instanceStats.get(config.configId));
            }
            
            allResults.put(instanceName, instanceResults);
            allStats.put(instanceName, instanceStats);
            
            // 保存该算例的结果
            saveInstanceResults(instanceName, instanceResults, instanceStats, 
                              machinePath, itemPath, configs);
        }
        
        long endTime = System.currentTimeMillis();
        double totalHours = (endTime - startTime) / 3600000.0;
        
        // 生成全局汇总
        System.out.println("\n生成全局汇总报告...");
        generateGlobalSummary(allResults, allStats, instances.length, configs);
        
        System.out.println("\n" + repeat("=", 80));
        System.out.println("完整消融实验完成！");
        System.out.println("  - 总耗时：" + String.format("%.2f小时", totalHours));
        System.out.println("  - 结果目录：chapter-1/src/main/output/final_ablation/");
        System.out.println(repeat("=", 80));
    }
    
    private static ExperimentResult runSingleExperiment(
            String machinePath, String itemPath, String instanceName,
            AblationConfig config, int runId) {
        
        try {
            Input input = ReadDataUtil.readData(machinePath, itemPath, true);
            long timeLimitMs = 5 * 60 * 1000;
            
            // 设置系统属性（用于控制禁忌搜索和评分策略）
            System.setProperty("ablation.useTabuSearch", String.valueOf(config.useTabuSearch));
            System.setProperty("ablation.useComplexScore", String.valueOf(config.useComplexScore));
            
            BatchGa batchGa;
            if (config.useLocalSearchMutation) {
                // 使用LocalSearch变异算子
                batchGa = new BatchGaAblation(
                    1000000000, 100, 5, 2, 0.2, 0.8,
                    input, true, "TabuSearch",
                    100, 10, 30, timeLimitMs, config.heuristicInitRatio
                );
            } else {
                // 使用简单变异算子
                batchGa = new BatchGaSimpleMutation(
                    1000000000, 100, 5, 2, 0.2, 0.8,
                    input, true, "TabuSearch",
                    100, 10, 30, timeLimitMs
                );
                batchGa.heuristicInitRatio = config.heuristicInitRatio;
            }
            
            long start = System.currentTimeMillis();
            Result result = batchGa.solve();
            long end = System.currentTimeMillis();
            
            ExperimentResult expResult = new ExperimentResult();
            expResult.configId = config.configId;
            expResult.configName = config.configName;
            expResult.instanceName = instanceName;
            expResult.runId = runId;
            expResult.cmax = batchGa.bestGenome.cMax;
            expResult.avgUtilization = calculateAvgUtilization(batchGa);
            expResult.timeMs = end - start;
            expResult.iterationHistory = result.getIreatorList();
            
            // 生成实验报告
            try {
                ExperimentResultWriter writer = new ExperimentResultWriter(
                    config.configName, runId, instanceName
                );
                
                writer.writeExperimentInfo(
                    config.configName + " - " + config.description,
                    runId, instanceName, 1000000, 100, 0.2, 0.8
                );
                
                writer.writeFinalResults(
                    batchGa.bestGenome.cMax,
                    end - start,
                    batchGa.t,
                    result.getIreatorList(),
                    batchGa.bestGenome,
                    batchGa.machines
                );
                
                writer.writeMachineLoads(batchGa.bestGenome);
                writer.close();
                
            } catch (Exception e) {
                System.err.println("生成报告失败: " + e.getMessage());
            }
            
            return expResult;
            
        } catch (Exception e) {
            System.err.println("实验运行失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private static double calculateAvgUtilization(BatchGa batchGa) {
        double totalUtilization = 0.0;
        int count = 0;
        
        for (int i = 0; i < batchGa.bestGenome.solutions.size(); i++) {
            ProblemFrame.BatchResult solution = batchGa.bestGenome.solutions.get(i);
            if (solution != null && solution.solutions != null) {
                for (ProblemFrame.Solution batch : solution.solutions) {
                    totalUtilization += batch.rate;
                    count++;
                }
            }
        }
        
        return count > 0 ? totalUtilization / count : 0.0;
    }
    
    private static Statistics calculateStatistics(
            AblationConfig config, String instanceName,
            List<ExperimentResult> results) {
        
        Statistics stats = new Statistics();
        stats.configId = config.configId;
        stats.configName = config.configName;
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
    
    private static void printStatistics(Statistics stats) {
        System.out.println("========================================");
        System.out.println(String.format("[%s] %s [%s]", 
            stats.configId, stats.configName, stats.instanceName));
        System.out.println("========================================");
        System.out.println(String.format("平均Cmax:      %.4f", stats.avgCmax));
        System.out.println(String.format("最小Cmax:      %.4f", stats.minCmax));
        System.out.println(String.format("最大Cmax:      %.4f", stats.maxCmax));
        System.out.println(String.format("标准差:        %.4f", stats.stdDevCmax));
        System.out.println(String.format("平均利用率:    %.2f%%", stats.avgUtilization * 100));
        System.out.println(String.format("平均时间:      %.2f ms", stats.avgTimeMs));
        if (stats.rpd != 0.0) {
            System.out.println(String.format("RPD:           %.2f%%", stats.rpd));
        }
        System.out.println("========================================\n");
    }
    
    private static void saveInstanceResults(
            String instanceName,
            Map<String, List<ExperimentResult>> results,
            Map<String, Statistics> stats,
            String machinePath,
            String itemPath,
            List<AblationConfig> configs) {
        
        String baseDir = "chapter-1/src/main/output/final_ablation/" + instanceName;
        new java.io.File(baseDir).mkdirs();
        
        // 1. 详细结果
        String detailPath = baseDir + "/detailed_results.csv";
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(detailPath));
            writer.println("ConfigID,ConfigName,RunID,Cmax,AvgUtilization,TimeMs");
            
            for (AblationConfig config : configs) {
                for (ExperimentResult result : results.get(config.configId)) {
                    writer.println(String.format("%s,%s,%d,%.4f,%.4f,%d",
                        config.configId, config.configName, result.runId, 
                        result.cmax, result.avgUtilization, result.timeMs));
                }
            }
            writer.close();
            System.out.println("详细结果已保存: " + detailPath);
        } catch (IOException e) {
            System.err.println("保存详细结果失败: " + e.getMessage());
        }
        
        // 2. 箱线图数据
        String boxplotPath = baseDir + "/boxplot_data.csv";
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(boxplotPath));
            writer.println("ConfigName,Cmax,AvgUtilization,TimeMs");
            
            for (AblationConfig config : configs) {
                for (ExperimentResult result : results.get(config.configId)) {
                    writer.println(String.format("%s,%.4f,%.4f,%.0f",
                        config.configName, result.cmax, result.avgUtilization, (double)result.timeMs));
                }
            }
            writer.close();
            System.out.println("箱线图数据已保存: " + boxplotPath);
        } catch (IOException e) {
            System.err.println("保存箱线图数据失败: " + e.getMessage());
        }
        
        // 3. 统计摘要（含RPD）
        String summaryPath = baseDir + "/summary_statistics.csv";
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(summaryPath));
            writer.println("ConfigID,ConfigName,AvgCmax,MinCmax,MaxCmax,StdDev,AvgUtilization,AvgTimeMs,RPD");
            
            for (AblationConfig config : configs) {
                Statistics s = stats.get(config.configId);
                writer.println(String.format("%s,%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f,%.2f",
                    config.configId, config.configName, s.avgCmax, s.minCmax, s.maxCmax,
                    s.stdDevCmax, s.avgUtilization, s.avgTimeMs, s.rpd));
            }
            writer.close();
            System.out.println("统计摘要已保存: " + summaryPath);
        } catch (IOException e) {
            System.err.println("保存统计摘要失败: " + e.getMessage());
        }
        
        // 4. 生成收敛曲线对比图
        try {
            Map<String, List<Double>> convergenceMap = new HashMap<String, List<Double>>();
            
            for (AblationConfig config : configs) {
                Statistics s = stats.get(config.configId);
                if (s.bestResult != null && s.bestResult.iterationHistory != null) {
                    convergenceMap.put(config.configName, s.bestResult.iterationHistory);
                }
            }
            
            if (!convergenceMap.isEmpty()) {
                String chartPath = baseDir + "/convergence_comparison.png";
                ChartGenerator.generateMultipleConvergenceCurves(
                    convergenceMap,
                    chartPath,
                    instanceName + " - 消融实验收敛曲线对比"
                );
                System.out.println("对比曲线图已保存: " + chartPath);
            }
        } catch (Exception e) {
            System.err.println("生成对比曲线图失败: " + e.getMessage());
        }
        
        // 5. 生成对比报告
        String reportPath = baseDir + "/ablation_report.txt";
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(reportPath));
            writer.println("================================================================================");
            writer.println("                      完整消融实验对比报告");
            writer.println("================================================================================");
            writer.println();
            writer.println("算例: " + instanceName);
            writer.println("运行次数: 10");
            writer.println();
            writer.println("================================================================================");
            writer.println("                           统计结果对比");
            writer.println("================================================================================");
            writer.println();
            writer.println(String.format("%-20s %-12s %-12s %-12s %-12s %-12s %-10s", 
                "配置", "平均Cmax", "最优Cmax", "最差Cmax", "标准差", "平均利用率", "RPD"));
            writer.println(repeat("-", 95));
            
            for (AblationConfig config : configs) {
                Statistics s = stats.get(config.configId);
                writer.println(String.format("%-20s %-12.4f %-12.4f %-12.4f %-12.4f %-12.2f%% %-10.2f%%", 
                    config.configName, s.avgCmax, s.minCmax, s.maxCmax, 
                    s.stdDevCmax, s.avgUtilization * 100, s.rpd));
            }
            
            writer.println();
            writer.println("================================================================================");
            writer.println("                           各组件贡献分析");
            writer.println("================================================================================");
            writer.println();
            
            Statistics full = stats.get("V0-Full");
            Statistics randInit = stats.get("V1-RandomInit");
            Statistics simpleMut = stats.get("V2-SimpleMutation");
            Statistics noTabu = stats.get("V3-NoTabu");
            Statistics simpleScore = stats.get("V4-SimpleScore");
            Statistics minimal = stats.get("V5-Minimal");
            
            writer.println("1. 初始化策略的贡献:");
            writer.println(String.format("   移除50%%启发式，性能下降: %.2f%%", randInit.rpd));
            writer.println();
            
            writer.println("2. LocalSearch变异算子的贡献:");
            writer.println(String.format("   改用简单变异，性能下降: %.2f%%", simpleMut.rpd));
            writer.println();
            
            writer.println("3. 禁忌搜索的贡献:");
            writer.println(String.format("   移除禁忌搜索，性能下降: %.2f%%", noTabu.rpd));
            writer.println();
            
            writer.println("4. 复杂评分策略的贡献:");
            writer.println(String.format("   改用简化评分，性能下降: %.2f%%", simpleScore.rpd));
            writer.println();
            
            writer.println("5. 总体影响:");
            writer.println(String.format("   移除所有组件，性能下降: %.2f%%", minimal.rpd));
            writer.println();
            
            writer.println("================================================================================");
            writer.println("说明:");
            writer.println("  - RPD = (当前配置Cmax - V0-Full的Cmax) / V0-Full的Cmax × 100%");
            writer.println("  - RPD越大，表示性能下降越多，说明该组件越重要");
            writer.println("================================================================================");
            
            writer.close();
        } catch (IOException e) {
            System.err.println("保存对比报告失败: " + e.getMessage());
        }
    }
    
    private static void generateGlobalSummary(
            Map<String, Map<String, List<ExperimentResult>>> allResults,
            Map<String, Map<String, Statistics>> allStats,
            int totalInstances,
            List<AblationConfig> configs) {
        
        String baseDir = "chapter-1/src/main/output/final_ablation/";
        
        // 1. 全局汇总CSV
        String summaryPath = baseDir + "global_summary.csv";
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(summaryPath));
            writer.println("Instance,ConfigID,ConfigName,AvgCmax,MinCmax,MaxCmax,StdDev,AvgUtilization,AvgTimeMs,RPD");
            
            for (String instance : allStats.keySet()) {
                Map<String, Statistics> stats = allStats.get(instance);
                for (AblationConfig config : configs) {
                    Statistics s = stats.get(config.configId);
                    writer.println(String.format("%s,%s,%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f,%.2f",
                        instance, config.configId, config.configName, s.avgCmax, s.minCmax, 
                        s.maxCmax, s.stdDevCmax, s.avgUtilization, s.avgTimeMs, s.rpd));
                }
            }
            writer.close();
            System.out.println("全局汇总CSV已保存: " + summaryPath);
        } catch (IOException e) {
            System.err.println("保存全局汇总失败: " + e.getMessage());
        }
        
        // 2. 全局平均RPD统计
        String avgRpdPath = baseDir + "average_rpd.csv";
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(avgRpdPath));
            writer.println("ConfigID,ConfigName,AvgRPD");
            
            for (AblationConfig config : configs) {
                double totalRpd = 0.0;
                int count = 0;
                
                for (String instance : allStats.keySet()) {
                    Statistics s = allStats.get(instance).get(config.configId);
                    totalRpd += s.rpd;
                    count++;
                }
                
                double avgRpd = count > 0 ? totalRpd / count : 0.0;
                writer.println(String.format("%s,%s,%.2f", 
                    config.configId, config.configName, avgRpd));
            }
            writer.close();
            System.out.println("平均RPD已保存: " + avgRpdPath);
        } catch (IOException e) {
            System.err.println("保存平均RPD失败: " + e.getMessage());
        }
        
        // 3. 全局汇总报告
        String reportPath = baseDir + "global_summary_report.txt";
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(reportPath));
            writer.println("================================================================================");
            writer.println("                   完整消融实验 - 全局汇总报告");
            writer.println("================================================================================");
            writer.println();
            writer.println("实验配置:");
            writer.println("  - 测试算例数: " + totalInstances);
            writer.println("  - 每配置运行次数: 10");
            writer.println("  - 消融组件: 初始化、变异算子、禁忌搜索、评分策略");
            writer.println();
            writer.println("配置说明:");
            for (AblationConfig config : configs) {
                writer.println(String.format("  %s: %s", config.configName, config.description));
            }
            writer.println();
            writer.println("================================================================================");
            writer.println("                           平均RPD统计");
            writer.println("================================================================================");
            writer.println();
            writer.println(String.format("%-20s %-10s", "配置", "平均RPD"));
            writer.println(repeat("-", 35));
            
            for (AblationConfig config : configs) {
                double totalRpd = 0.0;
                int count = 0;
                
                for (String instance : allStats.keySet()) {
                    Statistics s = allStats.get(instance).get(config.configId);
                    totalRpd += s.rpd;
                    count++;
                }
                
                double avgRpd = count > 0 ? totalRpd / count : 0.0;
                writer.println(String.format("%-20s %-10.2f%%", config.configName, avgRpd));
            }
            
            writer.println();
            writer.println("================================================================================");
            writer.println("结论:");
            writer.println("  - 查看各配置的平均RPD可以了解各组件的重要性");
            writer.println("  - RPD越大，说明该组件对算法性能的贡献越大");
            writer.println("  - 详细结果请查看: global_summary.csv");
            writer.println("================================================================================");
            
            writer.close();
            System.out.println("全局汇总报告已保存: " + reportPath);
        } catch (IOException e) {
            System.err.println("保存全局汇总报告失败: " + e.getMessage());
        }
    }
    
    // Java 8兼容的repeat方法
    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
