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
 * 消融实验运行器 (Ablation Study Runner)
 * 
 * 目的：验证局部搜索策略和启发式初始化策略的有效性
 * 
 * 实验组设计：
 * 1. 完整算法 (Full)        - 启发式初始化 + 局部搜索
 * 2. 无局部搜索 (No-LS)      - 启发式初始化 + 无局部搜索
 * 3. 无启发式初始化 (No-Init) - 随机初始化 + 局部搜索
 * 4. 基础GA (Basic)          - 随机初始化 + 无局部搜索
 * 
 * 每组运行10次，记录平均makespan、标准差、最优值、最差值
 */
public class AblationExperimentRunner {
    
    /**
     * 消融实验配置
     */
    static class AblationConfig {
        String name;                // 实验组名称
        String description;         // 描述
        boolean enableLocalSearch;  // 是否启用局部搜索
        boolean enableHeuristicInit;// 是否启用启发式初始化
        
        public AblationConfig(String name, String description, 
                             boolean enableLocalSearch, boolean enableHeuristicInit) {
            this.name = name;
            this.description = description;
            this.enableLocalSearch = enableLocalSearch;
            this.enableHeuristicInit = enableHeuristicInit;
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
        String baseDir = "src/main/resources/result/消融实验/";
        String folderName = experimentName + "_run" + runNo + "_" + 
                           new SimpleDateFormat("HHmmss").format(new Date());
        ExperimentResultWriter writer = new ExperimentResultWriter(
            folderName, runNo, problem
        );
        writer.writeExperimentInfo(experimentName, runNo, params);
        ga.setResultWriter(writer);
        
        System.out.println("    📁 结果将保存到: " + baseDir + folderName + "/");
        
        // 运行算法
        long startTime = System.currentTimeMillis();
        Solution solution = ga.solve();
        long endTime = System.currentTimeMillis();
        
        // 关闭输出器
        writer.close();
        
        System.out.println("    ✓ 运行完成！耗时: " + (endTime - startTime) + " ms");
        System.out.println("    📊 Makespan: " + String.format("%.2f", solution.cost));
        System.out.println("    💾 详细结果已保存（包括收敛曲线图、甘特图、打印布局图）");
        
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
     * 生成消融实验对比图表和报告
     */
    private static void generateAblationSummary(
            String instanceName,
            Map<String, Map<String, Object>> allResults) throws Exception {
        
        String baseDir = "src/main/resources/result/消融实验/";
        String summaryFolder = baseDir + "消融实验总结_" + instanceName + "/";
        
        File dir = new File(summaryFolder);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 准备数据
        String[] experimentNames = {"完整算法", "无局部搜索", "无启发式初始化", "基础GA"};
        double[] avgMakespans = new double[4];
        Map<String, List<Double>> boxData = new HashMap<>();
        
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
            summaryFolder + "消融实验对比柱状图.png",
            "消融实验对比 - 平均Makespan"
        );
        
        // 生成箱线图
        ChartGenerator.generateBoxPlot(
            boxData,
            summaryFolder + "消融实验箱线图.png",
            "消融实验Makespan分布"
        );
        
        // 生成文本报告
        PrintWriter writer = new PrintWriter(new FileWriter(summaryFolder + "消融实验报告.txt"));
        writer.println("================================================================================");
        writer.println("                        消融实验总结报告");
        writer.println("================================================================================");
        writer.println("算例: " + instanceName);
        writer.println("实验时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        writer.println();
        writer.println("【实验设计】");
        writer.println("目的: 验证局部搜索策略和启发式初始化策略的有效性");
        writer.println();
        writer.println("实验组:");
        writer.println("  1. 完整算法       - 启发式初始化 + 局部搜索 (基线)");
        writer.println("  2. 无局部搜索     - 启发式初始化 + 无局部搜索");
        writer.println("  3. 无启发式初始化 - 随机初始化 + 局部搜索");
        writer.println("  4. 基础GA         - 随机初始化 + 无局部搜索");
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
        writer.println("                        贡献度分析");
        writer.println("================================================================================");
        writer.println();
        
        // 计算各组件贡献度
        double fullAvg = (double) allResults.get("完整算法").get("avg");
        double noLSAvg = (double) allResults.get("无局部搜索").get("avg");
        double noInitAvg = (double) allResults.get("无启发式初始化").get("avg");
        double basicAvg = (double) allResults.get("基础GA").get("avg");
        
        double lsContribution = ((noLSAvg - fullAvg) / fullAvg) * 100.0;
        double initContribution = ((noInitAvg - fullAvg) / fullAvg) * 100.0;
        double combinedContribution = ((basicAvg - fullAvg) / fullAvg) * 100.0;
        
        writer.println("局部搜索策略贡献:");
        writer.println(String.format("  去除局部搜索后性能下降: %.2f%%", lsContribution));
        writer.println(String.format("  说明: 局部搜索使makespan降低 %.2f", noLSAvg - fullAvg));
        writer.println();
        
        writer.println("启发式初始化策略贡献:");
        writer.println(String.format("  去除启发式初始化后性能下降: %.2f%%", initContribution));
        writer.println(String.format("  说明: 启发式初始化使makespan降低 %.2f", noInitAvg - fullAvg));
        writer.println();
        
        writer.println("两者联合贡献:");
        writer.println(String.format("  两者都去除后性能下降: %.2f%%", combinedContribution));
        writer.println(String.format("  说明: 两者联合使makespan降低 %.2f", basicAvg - fullAvg));
        writer.println();
        
        writer.println("================================================================================");
        writer.println("                        统计显著性建议");
        writer.println("================================================================================");
        writer.println();
        writer.println("建议进行以下统计检验:");
        writer.println("  1. 配对t检验 (Paired t-test)");
        writer.println("     - 检验'完整算法' vs '无局部搜索'");
        writer.println("     - 检验'完整算法' vs '无启发式初始化'");
        writer.println("     - 检验'完整算法' vs '基础GA'");
        writer.println();
        writer.println("  2. 方差分析 (ANOVA)");
        writer.println("     - 检验四组间是否存在显著差异");
        writer.println();
        writer.println("  3. Wilcoxon符号秩检验 (非参数检验)");
        writer.println("     - 如果数据不满足正态分布假设");
        writer.println();
        writer.println("可使用SPSS、R或Python进行以上统计分析");
        writer.println();
        
        writer.println("================================================================================");
        writer.close();
        
        System.out.println("\n✅ 消融实验总结已生成: " + summaryFolder);
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
     * 主函数：运行完整消融实验
     */
    public static void main(String[] args) {
        // 测试算例
        String instancePath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\instance\\J100\\J100P4B2D10_05.txt";
        String instanceName = "J100P4B2D10_05";
        int numberOfRuns = 10;  // 每组运行次数
        
        // 🚀 多线程配置
        // 建议：线程数 = CPU核心数，或根据内存情况调整
        // 如果内存不足，减少线程数；如果CPU够强，可以增加
        int threadCount = Math.min(numberOfRuns, Runtime.getRuntime().availableProcessors());
        // 也可以手动设置：int threadCount = 4;
        
        System.out.println("================================================================================");
        System.out.println("                   消融实验 (Ablation Study) - 多线程加速版");
        System.out.println("================================================================================");
        System.out.println("算例: " + instanceName);
        System.out.println("每组运行次数: " + numberOfRuns);
        System.out.println("实验组数: 4");
        System.out.println("预计总运行次数: " + (4 * numberOfRuns));
        System.out.println("🚀 并行线程数: " + threadCount + " (CPU核心数: " + Runtime.getRuntime().availableProcessors() + ")");
        System.out.println("💡 预计加速比: " + String.format("%.1fx", Math.min(threadCount, numberOfRuns) * 1.0));
        System.out.println("================================================================================\n");
        
        // 定义4组消融实验配置
        AblationConfig[] configs = {
            new AblationConfig("完整算法", "启发式初始化 + 局部搜索", true, true),
            new AblationConfig("无局部搜索", "启发式初始化 + 无局部搜索", false, true),
            new AblationConfig("无启发式初始化", "随机初始化 + 局部搜索", true, false),
            new AblationConfig("基础GA", "随机初始化 + 无局部搜索", false, false)
        };
        
        // 存储所有实验结果
        Map<String, Map<String, Object>> allResults = new LinkedHashMap<>();
        
        try {
            // 依次运行4组实验
            for (AblationConfig config : configs) {
                System.out.println("\n" + repeatString("█", 80));
                System.out.println("  实验组: " + config.name);
                System.out.println("  描述: " + config.description);
                System.out.println(repeatString("█", 80));
                
                // 创建参数配置
                GAParameters params = GAParameters.createAblationParameters(
                    config.enableLocalSearch,
                    config.enableHeuristicInit
                );
                
                // 运行多次实验（多线程）
                Map<String, Object> results = runMultipleExperiments(
                    instancePath,
                    config.name,
                    params,
                    numberOfRuns,
                    threadCount  // 传递线程数
                );
                
                allResults.put(config.name, results);
            }
            
            // 生成对比图表和总结报告
            System.out.println("\n" + repeatString("=", 80));
            System.out.println("所有实验组完成，正在生成总结报告...");
            System.out.println(repeatString("=", 80));
            
            generateAblationSummary(instanceName, allResults);
            
            System.out.println("\n" + repeatString("=", 80));
            System.out.println("                    消融实验全部完成！");
            System.out.println(repeatString("=", 80));
            System.out.println("\n📂 结果文件结构:");
            System.out.println("src/main/resources/result/消融实验/");
            System.out.println("├── 完整算法_run1_HHMMSS/");
            System.out.println("│   ├── 实验报告.txt");
            System.out.println("│   ├── 收敛曲线图.png");
            System.out.println("│   ├── 甘特图.png");
            System.out.println("│   └── 打印布局图*.png");
            System.out.println("├── 完整算法_run2_HHMMSS/");
            System.out.println("├── ... (共 " + (4 * numberOfRuns) + " 个独立文件夹)");
            System.out.println("└── 消融实验总结_" + instanceName + "/");
            System.out.println("    ├── 消融实验报告.txt         ← 核心总结报告");
            System.out.println("    ├── 消融实验对比柱状图.png");
            System.out.println("    └── 消融实验箱线图.png");
            System.out.println("\n💡 提示:");
            System.out.println("  - 每次运行的详细结果都已单独保存");
            System.out.println("  - 查看 '消融实验总结_" + instanceName + "/' 文件夹获取整体分析");
            System.out.println(repeatString("=", 80));
        } catch (Exception e) {
            System.err.println("❌ 消融实验执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
