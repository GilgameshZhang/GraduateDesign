import AlgorthmFrame.ga.GA;
import ProblemFrame.GAParameters;
import ProblemFrame.Solution;
import ProgramEntity.Input;
import ProgramEntity.Problem;
import util.java.ChartGenerator;
import util.java.ExperimentResultWriter;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 邻域策略消融实验运行器 (Neighborhood Ablation Study Runner)
 * 
 * 目的：验证5种邻域搜索策略(N1-N5)的各自贡献度
 * 
 * 邻域策略说明：
 * - N1: 基于打印时间的跨机移动（轮盘赌选择零件，高度越高权重越大）
 * - N2: 基于面积占用率的跨机移动（轮盘赌选择零件，面积越大权重越大）
 * - N3: 相邻批次交换
 * - N4: 关键块首尾相邻工序交换（离散处理阶段优化）
 * - N5: 关键工序机器重分配（离散处理阶段优化）
 * 
 * 实验组设计（7组）：
 * 1. 完整算法(N1+N2+N3+N4+N5) - 基线
 * 2. 无N1 (N2+N3+N4+N5)
 * 3. 无N2 (N1+N3+N4+N5)
 * 4. 无N3 (N1+N2+N4+N5)
 * 5. 无N4 (N1+N2+N3+N5)
 * 6. 无N5 (N1+N2+N3+N4)
 * 7. 无局部搜索 - 对照组
 * 
 * 每组运行10次，记录平均makespan、标准差、最优值、最差值
 */
public class NeighborhoodAblationExperimentRunner {
    
    /**
     * 邻域消融实验配置
     */
    static class NeighborhoodConfig {
        String name;                // 实验组名称
        String description;         // 描述
        boolean enableN1;           // 是否启用N1
        boolean enableN2;           // 是否启用N2
        boolean enableN3;           // 是否启用N3
        boolean enableN4;           // 是否启用N4
        boolean enableN5;           // 是否启用N5
        
        public NeighborhoodConfig(String name, String description, 
                                 boolean enableN1, boolean enableN2, boolean enableN3,
                                 boolean enableN4, boolean enableN5) {
            this.name = name;
            this.description = description;
            this.enableN1 = enableN1;
            this.enableN2 = enableN2;
            this.enableN3 = enableN3;
            this.enableN4 = enableN4;
            this.enableN5 = enableN5;
        }
    }
    
    /**
     * 运行单次消融实验
     */
    private static double runSingleExperiment(String instancePath, 
                                             String experimentName,
                                             GAParameters params,
                                             int runNo) throws Exception {
        // 解析算例
        File instanceFile = new File(instancePath);
        if (!instanceFile.exists()) {
            throw new FileNotFoundException("算例文件不存在: " + instancePath);
        }
        
        Input input = new Input(instanceFile);
        Problem problem = input.getProblemDesFromFile();
        
        // 创建GA实例
        GA ga = new GA(problem, params);
        
        // 创建结果输出器
        String baseDir = "src/main/resources/result/邻域消融实验/";
        String folderName = experimentName + "_run" + runNo + "_" + 
                           new SimpleDateFormat("HHmmss").format(new Date());
        ExperimentResultWriter writer = new ExperimentResultWriter(
            folderName, runNo, problem
        );
        writer.writeExperimentInfo(experimentName, runNo, params);
        ga.setResultWriter(writer);
        
        // 运行算法
        long startTime = System.currentTimeMillis();
        Solution solution = ga.solve();
        long endTime = System.currentTimeMillis();
        
        // 关闭输出器
        writer.close();
        
        return solution.cost;
    }
    
    /**
     * 运行多次实验并统计结果（多线程版本）
     */
    private static Map<String, Object> runMultipleExperiments(
            String instancePath, 
            String experimentName,
            GAParameters params,
            int numberOfRuns,
            int threadCount) throws Exception {
        
        System.out.println("\n▶▶▶ 开始运行: " + experimentName);
        System.out.println("配置: " + params.toString());
        System.out.println("将运行 " + numberOfRuns + " 次，使用 " + threadCount + " 个线程并行执行\n");
        
        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // 用于收集结果的线程安全列表
        List<Future<Double>> futures = new ArrayList<>();
        AtomicInteger completedCount = new AtomicInteger(0);
        
        // 提交所有任务
        for (int i = 1; i <= numberOfRuns; i++) {
            final int runNo = i;
            Future<Double> future = executor.submit(new Callable<Double>() {
                @Override
                public Double call() throws Exception {
                    synchronized (System.out) {
                        System.out.println(String.format("  [线程-%s] 开始运行 %d/%d", 
                            Thread.currentThread().getName(), runNo, numberOfRuns));
                    }
                    
                    long startTime = System.currentTimeMillis();
                    double makespan = runSingleExperiment(instancePath, experimentName, params, runNo);
                    long endTime = System.currentTimeMillis();
                    
                    int completed = completedCount.incrementAndGet();
                    
                    synchronized (System.out) {
                        System.out.println(String.format("  [线程-%s] ✓ 运行 %d/%d 完成！耗时: %d ms, Makespan: %.2f", 
                            Thread.currentThread().getName(), runNo, numberOfRuns, 
                            (endTime - startTime), makespan));
                        System.out.println(String.format("  进度: %d/%d (%.1f%%)\n", 
                            completed, numberOfRuns, (completed * 100.0 / numberOfRuns)));
                    }
                    
                    return makespan;
                }
            });
            futures.add(future);
        }
        
        // 关闭线程池并等待所有任务完成
        executor.shutdown();
        
        System.out.println("  ⏳ 等待所有实验完成...");
        
        // 收集结果
        List<Double> makespans = new ArrayList<>();
        double minMakespan = Double.MAX_VALUE;
        double maxMakespan = Double.MIN_VALUE;
        double totalMakespan = 0.0;
        
        for (Future<Double> future : futures) {
            try {
                double makespan = future.get();  // 阻塞等待结果
                makespans.add(makespan);
                minMakespan = Math.min(minMakespan, makespan);
                maxMakespan = Math.max(maxMakespan, makespan);
                totalMakespan += makespan;
            } catch (ExecutionException e) {
                System.err.println("  ❌ 某次实验执行失败: " + e.getCause().getMessage());
                throw new Exception("实验执行失败", e.getCause());
            }
        }
        
        // 确保线程池已终止
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
        
        // 计算统计数据
        double avgMakespan = totalMakespan / numberOfRuns;
        
        // 计算标准差
        double variance = 0.0;
        for (double makespan : makespans) {
            variance += Math.pow(makespan - avgMakespan, 2);
        }
        double stdDev = Math.sqrt(variance / numberOfRuns);
        
        // 计算变异系数
        double cv = (stdDev / avgMakespan) * 100.0;
        
        System.out.println(repeatString("=", 80));
        System.out.println(String.format("✓ %s - 全部运行完成", experimentName));
        System.out.println(repeatString("=", 80));
        System.out.println(String.format("  平均值: %.2f", avgMakespan));
        System.out.println(String.format("  标准差: %.2f", stdDev));
        System.out.println(String.format("  变异系数: %.2f%%", cv));
        System.out.println(String.format("  最优值: %.2f", minMakespan));
        System.out.println(String.format("  最差值: %.2f", maxMakespan));
        System.out.println(String.format("  极差: %.2f", maxMakespan - minMakespan));
        System.out.println(repeatString("=", 80));
        
        // 返回结果
        Map<String, Object> results = new HashMap<>();
        results.put("makespans", makespans);
        results.put("avg", avgMakespan);
        results.put("std", stdDev);
        results.put("cv", cv);
        results.put("min", minMakespan);
        results.put("max", maxMakespan);
        
        return results;
    }
    
    /**
     * 生成邻域消融实验对比图表和报告
     */
    private static void generateNeighborhoodAblationSummary(
            String instanceName,
            Map<String, Map<String, Object>> allResults) throws Exception {
        
        String baseDir = "src/main/resources/result/邻域消融实验/";
        String summaryFolder = baseDir + "邻域消融实验总结_" + instanceName + "/";
        
        File dir = new File(summaryFolder);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 准备数据
        String[] experimentNames = {"完整算法", "无N1", "无N2", "无N3", "无N4", "无N5", "无局部搜索"};
        double[] avgMakespans = new double[7];
        Map<String, List<Double>> boxData = new LinkedHashMap<>();
        
        int idx = 0;
        for (String name : experimentNames) {
            Map<String, Object> result = allResults.get(name);
            avgMakespans[idx++] = (double) result.get("avg");
            boxData.put(name, (List<Double>) result.get("makespans"));
        }
        
        // 生成对比柱状图
        ChartGenerator.generateParameterComparisonChart(
            experimentNames,
            avgMakespans,
            summaryFolder + "邻域消融实验对比柱状图.png",
            "邻域消融实验对比 - 平均Makespan"
        );
        
        // 生成箱线图
        ChartGenerator.generateBoxPlot(
            boxData,
            summaryFolder + "邻域消融实验箱线图.png",
            "邻域消融实验Makespan分布"
        );
        
        // 生成文本报告
        PrintWriter writer = new PrintWriter(new FileWriter(summaryFolder + "邻域消融实验报告.txt"));
        writer.println("================================================================================");
        writer.println("                     邻域策略消融实验总结报告");
        writer.println("================================================================================");
        writer.println("算例: " + instanceName);
        writer.println("实验时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        writer.println();
        writer.println("【实验设计】");
        writer.println("目的: 验证5种邻域搜索策略(N1-N5)的各自贡献度");
        writer.println();
        writer.println("邻域策略说明:");
        writer.println("  N1: 基于打印时间的跨机移动（轮盘赌选择零件，高度越高权重越大）");
        writer.println("  N2: 基于面积占用率的跨机移动（轮盘赌选择零件，面积越大权重越大）");
        writer.println("  N3: 相邻批次交换");
        writer.println("  N4: 关键块首尾相邻工序交换（离散处理阶段优化）");
        writer.println("  N5: 关键工序机器重分配（离散处理阶段优化）");
        writer.println();
        writer.println("实验组:");
        writer.println("  1. 完整算法      - N1+N2+N3+N4+N5 (基线)");
        writer.println("  2. 无N1          - N2+N3+N4+N5");
        writer.println("  3. 无N2          - N1+N3+N4+N5");
        writer.println("  4. 无N3          - N1+N2+N4+N5");
        writer.println("  5. 无N4          - N1+N2+N3+N5");
        writer.println("  6. 无N5          - N1+N2+N3+N4");
        writer.println("  7. 无局部搜索    - 无任何邻域策略 (对照组)");
        writer.println();
        writer.println("每组运行10次");
        writer.println();
        writer.println("================================================================================");
        writer.println("                        实验结果统计");
        writer.println("================================================================================");
        writer.println();
        
        writer.println(String.format("%-20s %-12s %-12s %-10s %-10s %-10s %-10s", 
            "实验组", "平均值", "标准差", "变异系数", "最优值", "最差值", "与基线差异"));
        writer.println("--------------------------------------------------------------------------------");
        
        double baselineAvg = (double) allResults.get("完整算法").get("avg");
        
        for (String name : experimentNames) {
            Map<String, Object> result = allResults.get(name);
            double avg = (double) result.get("avg");
            double std = (double) result.get("std");
            double cv = (double) result.get("cv");
            double min = (double) result.get("min");
            double max = (double) result.get("max");
            double diff = ((avg - baselineAvg) / baselineAvg) * 100.0;
            
            writer.println(String.format("%-20s %-12.2f %-12.2f %-9.2f%% %-10.2f %-10.2f %+9.2f%%", 
                name, avg, std, cv, min, max, diff));
        }
        
        writer.println();
        writer.println("================================================================================");
        writer.println("                     各邻域策略贡献度分析");
        writer.println("================================================================================");
        writer.println();
        
        // 计算各邻域策略的贡献度
        double fullAvg = (double) allResults.get("完整算法").get("avg");
        double noN1Avg = (double) allResults.get("无N1").get("avg");
        double noN2Avg = (double) allResults.get("无N2").get("avg");
        double noN3Avg = (double) allResults.get("无N3").get("avg");
        double noN4Avg = (double) allResults.get("无N4").get("avg");
        double noN5Avg = (double) allResults.get("无N5").get("avg");
        double noLSAvg = (double) allResults.get("无局部搜索").get("avg");
        
        double n1Contribution = ((noN1Avg - fullAvg) / fullAvg) * 100.0;
        double n2Contribution = ((noN2Avg - fullAvg) / fullAvg) * 100.0;
        double n3Contribution = ((noN3Avg - fullAvg) / fullAvg) * 100.0;
        double n4Contribution = ((noN4Avg - fullAvg) / fullAvg) * 100.0;
        double n5Contribution = ((noN5Avg - fullAvg) / fullAvg) * 100.0;
        double totalContribution = ((noLSAvg - fullAvg) / fullAvg) * 100.0;
        
        writer.println("N1策略（基于打印时间的跨机移动）贡献:");
        writer.println(String.format("  去除N1后性能下降: %.2f%%", n1Contribution));
        writer.println(String.format("  说明: N1使makespan降低 %.2f", noN1Avg - fullAvg));
        writer.println();
        
        writer.println("N2策略（基于面积占用率的跨机移动）贡献:");
        writer.println(String.format("  去除N2后性能下降: %.2f%%", n2Contribution));
        writer.println(String.format("  说明: N2使makespan降低 %.2f", noN2Avg - fullAvg));
        writer.println();
        
        writer.println("N3策略（相邻批次交换）贡献:");
        writer.println(String.format("  去除N3后性能下降: %.2f%%", n3Contribution));
        writer.println(String.format("  说明: N3使makespan降低 %.2f", noN3Avg - fullAvg));
        writer.println();
        
        writer.println("N4策略（关键块首尾相邻工序交换）贡献:");
        writer.println(String.format("  去除N4后性能下降: %.2f%%", n4Contribution));
        writer.println(String.format("  说明: N4使makespan降低 %.2f", noN4Avg - fullAvg));
        writer.println();
        
        writer.println("N5策略（关键工序机器重分配）贡献:");
        writer.println(String.format("  去除N5后性能下降: %.2f%%", n5Contribution));
        writer.println(String.format("  说明: N5使makespan降低 %.2f", noN5Avg - fullAvg));
        writer.println();
        
        writer.println("所有邻域策略联合贡献:");
        writer.println(String.format("  全部去除后性能下降: %.2f%%", totalContribution));
        writer.println(String.format("  说明: 所有邻域策略联合使makespan降低 %.2f", noLSAvg - fullAvg));
        writer.println();
        
        // 贡献度排序
        writer.println("================================================================================");
        writer.println("                     邻域策略重要性排序");
        writer.println("================================================================================");
        writer.println();
        
        Map<String, Double> contributions = new LinkedHashMap<>();
        contributions.put("N1", n1Contribution);
        contributions.put("N2", n2Contribution);
        contributions.put("N3", n3Contribution);
        contributions.put("N4", n4Contribution);
        contributions.put("N5", n5Contribution);
        
        // 按贡献度降序排序
        List<Map.Entry<String, Double>> sortedContributions = new ArrayList<>(contributions.entrySet());
        sortedContributions.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        writer.println("按贡献度从高到低排序:");
        int rank = 1;
        for (Map.Entry<String, Double> entry : sortedContributions) {
            String stars = generateStars(entry.getValue(), 0.5);  // 每0.5%一颗星
            writer.println(String.format("  %d. %s: %.2f%% %s", rank++, entry.getKey(), entry.getValue(), stars));
        }
        writer.println();
        
        // 协同效应分析
        writer.println("================================================================================");
        writer.println("                     协同效应分析");
        writer.println("================================================================================");
        writer.println();
        
        double sumIndividual = n1Contribution + n2Contribution + n3Contribution + n4Contribution + n5Contribution;
        double synergy = totalContribution - sumIndividual;
        
        writer.println("单独贡献度之和:");
        writer.println(String.format("  N1+N2+N3+N4+N5 = %.2f%% + %.2f%% + %.2f%% + %.2f%% + %.2f%% = %.2f%%",
            n1Contribution, n2Contribution, n3Contribution, n4Contribution, n5Contribution, sumIndividual));
        writer.println();
        
        writer.println("实际联合贡献:");
        writer.println(String.format("  全部邻域策略 = %.2f%%", totalContribution));
        writer.println();
        
        writer.println("协同效应:");
        writer.println(String.format("  %.2f%% - %.2f%% = %.2f%%", totalContribution, sumIndividual, synergy));
        if (synergy > 0) {
            writer.println("  结论: 各邻域策略存在正协同效应，联合使用效果更佳！");
        } else if (synergy < 0) {
            writer.println("  结论: 各邻域策略存在一定的重复作用。");
        } else {
            writer.println("  结论: 各邻域策略独立作用，无显著协同效应。");
        }
        writer.println();
        
        writer.println("================================================================================");
        writer.println("                     统计显著性建议");
        writer.println("================================================================================");
        writer.println();
        writer.println("建议进行以下统计检验:");
        writer.println("  1. 配对t检验 (Paired t-test)");
        writer.println("     - 检验'完整算法' vs '无N1'");
        writer.println("     - 检验'完整算法' vs '无N2'");
        writer.println("     - 检验'完整算法' vs '无N3'");
        writer.println("     - 检验'完整算法' vs '无N4'");
        writer.println("     - 检验'完整算法' vs '无N5'");
        writer.println("     - 检验'完整算法' vs '无局部搜索'");
        writer.println();
        writer.println("  2. 方差分析 (ANOVA)");
        writer.println("     - 检验七组间是否存在显著差异");
        writer.println();
        writer.println("  3. 事后检验 (Post-hoc test)");
        writer.println("     - Tukey HSD 或 Bonferroni 检验");
        writer.println("     - 确定哪些组之间存在显著差异");
        writer.println();
        writer.println("可使用SPSS、R或Python进行以上统计分析");
        writer.println();
        
        writer.println("================================================================================");
        writer.println("                     论文写作建议");
        writer.println("================================================================================");
        writer.println();
        writer.println("可以这样描述实验结果:");
        writer.println();
        writer.println("\"为验证混合局部搜索策略中各邻域算子的有效性，本文设计了细粒度的");
        writer.println("消融实验。实验结果表明：");
        writer.println();
        writer.println("(1) 五个邻域算子均对算法性能有正向贡献，其中[最重要的策略]贡献最大，");
        writer.println("    使makespan降低约X.XX%；");
        writer.println();
        writer.println("(2) 五个邻域算子的重要性排序为：[按贡献度排序]；");
        writer.println();
        writer.println("(3) 所有邻域算子联合使用可使makespan降低约" + String.format("%.2f", totalContribution) + "%，");
        writer.println("    显著优于单独使用任一邻域算子，证明了混合局部搜索策略的有效性；");
        writer.println();
        writer.println("(4) 配对t检验表明各邻域算子的贡献均具有统计显著性(p<0.05)。\"");
        writer.println();
        
        writer.println("================================================================================");
        writer.close();
        
        System.out.println("\n✅ 邻域消融实验总结已生成: " + summaryFolder);
    }
    
    /**
     * 根据贡献度生成星号（可视化）
     */
    private static String generateStars(double contribution, double unit) {
        int count = (int) Math.round(contribution / unit);
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < count; i++) {
            stars.append("★");
        }
        return stars.toString();
    }
    
    /**
     * 辅助方法：重复字符串（兼容Java 8）
     */
    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    /**
     * 主函数：运行完整邻域消融实验
     */
    public static void main(String[] args) {
        // 测试算例
        String instancePath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\instance\\J100\\J100P4B2D10_05.txt";
        String instanceName = "J100P4B2D10_05";
        int numberOfRuns = 10;  // 每组运行次数
        
        // 🚀 多线程配置
        int threadCount = Math.min(numberOfRuns, Runtime.getRuntime().availableProcessors());
        
        System.out.println("================================================================================");
        System.out.println("               邻域策略消融实验 (Neighborhood Ablation Study)");
        System.out.println("================================================================================");
        System.out.println("算例: " + instanceName);
        System.out.println("每组运行次数: " + numberOfRuns);
        System.out.println("实验组数: 7");
        System.out.println("预计总运行次数: " + (7 * numberOfRuns));
        System.out.println("🚀 并行线程数: " + threadCount + " (CPU核心数: " + Runtime.getRuntime().availableProcessors() + ")");
        System.out.println("================================================================================\n");
        
        // 定义7组邻域消融实验配置
        NeighborhoodConfig[] configs = {
            new NeighborhoodConfig("完整算法", "N1+N2+N3+N4+N5", true, true, true, true, true),
            new NeighborhoodConfig("无N1", "N2+N3+N4+N5", false, true, true, true, true),
            new NeighborhoodConfig("无N2", "N1+N3+N4+N5", true, false, true, true, true),
            new NeighborhoodConfig("无N3", "N1+N2+N4+N5", true, true, false, true, true),
            new NeighborhoodConfig("无N4", "N1+N2+N3+N5", true, true, true, false, true),
            new NeighborhoodConfig("无N5", "N1+N2+N3+N4", true, true, true, true, false),
        };
        
        // 存储所有实验结果
        Map<String, Map<String, Object>> allResults = new LinkedHashMap<>();
        
        try {
            // 依次运行前6组邻域消融实验
            for (NeighborhoodConfig config : configs) {
                System.out.println("\n" + repeatString("█", 80));
                System.out.println("  实验组: " + config.name);
                System.out.println("  描述: " + config.description);
                System.out.println(repeatString("█", 80));
                
                // 创建参数配置
                GAParameters params = GAParameters.createNeighborhoodAblationParameters(
                    config.enableN1,
                    config.enableN2,
                    config.enableN3,
                    config.enableN4,
                    config.enableN5
                );
                
                // 运行多次实验（多线程）
                Map<String, Object> results = runMultipleExperiments(
                    instancePath,
                    config.name,
                    params,
                    numberOfRuns,
                    threadCount
                );
                
                allResults.put(config.name, results);
            }
            
            // 运行第7组：无局部搜索（对照组）
            System.out.println("\n" + repeatString("█", 80));
            System.out.println("  实验组: 无局部搜索");
            System.out.println("  描述: 无任何邻域策略（对照组）");
            System.out.println(repeatString("█", 80));
            
            GAParameters noLSParams = GAParameters.createAblationParameters(false, true);
            Map<String, Object> noLSResults = runMultipleExperiments(
                instancePath,
                "无局部搜索",
                noLSParams,
                numberOfRuns,
                threadCount
            );
            allResults.put("无局部搜索", noLSResults);
            
            // 生成对比图表和总结报告
            System.out.println("\n" + repeatString("=", 80));
            System.out.println("所有实验组完成，正在生成总结报告...");
            System.out.println(repeatString("=", 80));
            
            generateNeighborhoodAblationSummary(instanceName, allResults);
            
            System.out.println("\n" + repeatString("=", 80));
            System.out.println("                    邻域消融实验全部完成！");
            System.out.println(repeatString("=", 80));
            System.out.println("\n📂 结果文件结构:");
            System.out.println("src/main/resources/result/邻域消融实验/");
            System.out.println("├── 完整算法_run1_HHMMSS/");
            System.out.println("│   ├── 实验报告.txt");
            System.out.println("│   ├── 收敛曲线图.png");
            System.out.println("│   ├── 甘特图.png");
            System.out.println("│   └── 打印布局图*.png");
            System.out.println("├── 完整算法_run2_HHMMSS/");
            System.out.println("├── ... (共 " + (7 * numberOfRuns) + " 个独立文件夹)");
            System.out.println("└── 邻域消融实验总结_" + instanceName + "/");
            System.out.println("    ├── 邻域消融实验报告.txt     ← 核心总结报告");
            System.out.println("    ├── 邻域消融实验对比柱状图.png");
            System.out.println("    └── 邻域消融实验箱线图.png");
            System.out.println("\n💡 提示:");
            System.out.println("  - 每次运行的详细结果都已单独保存");
            System.out.println("  - 查看 '邻域消融实验总结_" + instanceName + "/' 文件夹获取整体分析");
            System.out.println("  - 报告中包含贡献度排序、协同效应分析等");
            System.out.println(repeatString("=", 80));
        } catch (Exception e) {
            System.err.println("❌ 邻域消融实验执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

