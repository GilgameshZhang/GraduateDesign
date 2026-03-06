import AlgorthmFrame.ga.GA;
import AlgorthmFrame.pso.PSO;
import ProblemFrame.GAParameters;
import ProblemFrame.PSOParameters;
import ProblemFrame.Solution;
import ProgramEntity.Input;
import ProgramEntity.Problem;
import util.java.ExperimentResultWriter;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 算法对比实验运行器
 * 
 * 功能：
 * 1. 同时运行混合GA算法和PSO算法
 * 2. 使用相同的算例和运行次数
 * 3. 生成对比分析报告和图表
 * 4. 统计显著性检验建议
 */
public class AlgorithmComparisonRunner {
    
    // 算例配置
    private static final String BASE_DIR = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\instance\\";
    private static final String RESULT_BASE_DIR = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\src\\main\\resources\\result\\对比试验\\";
    
    // 实验配置
    private static final int RUNS_PER_INSTANCE = 10;
    
    /**
     * 算法类型
     */
    enum AlgorithmType {
        HYBRID_GA("混合GA", "混合GA\\"),
        PSO("PSO", "PSO\\");
        
        String name;
        String folder;
        
        AlgorithmType(String name, String folder) {
            this.name = name;
            this.folder = folder;
        }
    }
    
    /**
     * 算例信息
     */
    static class InstanceInfo {
        String category;
        String fullPath;
        String name;
        
        public InstanceInfo(String category, File file) {
            this.category = category;
            this.fullPath = file.getAbsolutePath();
            this.name = file.getName().replace(".txt", "");
        }
        
        public boolean isValid() {
            return fullPath != null && new File(fullPath).exists();
        }
    }
    
    /**
     * 运行单个算法的单次实验
     */
    private static double runSingleAlgorithm(String instancePath, String instanceName,
                                            int runNo, AlgorithmType algType,
                                            Object params) throws Exception {
        File instanceFile = new File(instancePath);
        Input input = new Input(instanceFile);
        Problem problem = input.getProblemDesFromFile();
        
        // 创建结果输出器
        String folderName = instanceName + "_run" + runNo;
        String fullOutputPath = RESULT_BASE_DIR + algType.folder + instanceName + "\\" + folderName;
        
        ExperimentResultWriter writer = new ExperimentResultWriter(fullOutputPath, runNo, problem);
        
        Solution solution;
        long startTime = System.currentTimeMillis();
        
        if (algType == AlgorithmType.HYBRID_GA) {
            GA ga = new GA(problem, (GAParameters) params);
            writer.writeExperimentInfo(algType.name, runNo, params.toString());
            ga.setResultWriter(writer);
            solution = ga.solve();
        } else {  // PSO
            PSO pso = new PSO(problem, (PSOParameters) params);
            writer.writeExperimentInfo(algType.name, runNo, params.toString());
            pso.setResultWriter(writer);
            solution = pso.solve();
        }
        
        long endTime = System.currentTimeMillis();
        writer.close();
        
        return solution.cost;
    }
    
    /**
     * 运行单个算例的多次实验（多线程）
     */
    private static Map<String, Object> runInstanceExperiments(InstanceInfo instance,
                                                              AlgorithmType algType,
                                                              int threadCount,
                                                              Object params) throws Exception {
        System.out.println("\n" + repeatString("=", 100));
        System.out.println(String.format("▶▶▶ [%s] 算例: %s", algType.name, instance.name));
        System.out.println("    运行次数: " + RUNS_PER_INSTANCE + " 次");
        System.out.println("    并行线程: " + threadCount);
        System.out.println(repeatString("=", 100));
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Double>> futures = new ArrayList<>();
        AtomicInteger completedCount = new AtomicInteger(0);
        
        for (int i = 1; i <= RUNS_PER_INSTANCE; i++) {
            final int runNo = i;
            Future<Double> future = executor.submit(new Callable<Double>() {
                @Override
                public Double call() throws Exception {
                    synchronized (System.out) {
                        System.out.println(String.format("  [线程-%s] [%s] 开始 %s 第 %d/%d 次运行", 
                            Thread.currentThread().getName(), algType.name, instance.name, runNo, RUNS_PER_INSTANCE));
                    }
                    
                    double makespan = runSingleAlgorithm(instance.fullPath, instance.name, runNo, algType, params);
                    
                    int completed = completedCount.incrementAndGet();
                    
                    synchronized (System.out) {
                        System.out.println(String.format("  [线程-%s] ✓ [%s] %s 第 %d/%d 次完成! Makespan: %.2f", 
                            Thread.currentThread().getName(), algType.name, instance.name, runNo, RUNS_PER_INSTANCE, makespan));
                        System.out.println(String.format("  进度: %d/%d (%.1f%%)\n", 
                            completed, RUNS_PER_INSTANCE, (completed * 100.0 / RUNS_PER_INSTANCE)));
                    }
                    
                    return makespan;
                }
            });
            futures.add(future);
        }
        
        executor.shutdown();
        
        List<Double> makespans = new ArrayList<>();
        double minMakespan = Double.MAX_VALUE;
        double totalMakespan = 0.0;
        
        for (Future<Double> future : futures) {
            try {
                double makespan = future.get();
                makespans.add(makespan);
                minMakespan = Math.min(minMakespan, makespan);
                totalMakespan += makespan;
            } catch (ExecutionException e) {
                throw new Exception("实验执行失败", e.getCause());
            }
        }
        
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        double avgMakespan = totalMakespan / RUNS_PER_INSTANCE;
        double variance = 0.0;
        for (double makespan : makespans) {
            variance += Math.pow(makespan - avgMakespan, 2);
        }
        double stdDev = Math.sqrt(variance / RUNS_PER_INSTANCE);
        
        System.out.println(repeatString("-", 100));
        System.out.println(String.format("✓ [%s] %s 完成 - 平均: %.2f, 最优: %.2f, 标准差: %.2f", 
            algType.name, instance.name, avgMakespan, minMakespan, stdDev));
        System.out.println(repeatString("-", 100));
        
        Map<String, Object> results = new HashMap<>();
        results.put("algorithm", algType.name);
        results.put("instanceName", instance.name);
        results.put("makespans", makespans);
        results.put("avg", avgMakespan);
        results.put("std", stdDev);
        results.put("min", minMakespan);
        
        return results;
    }
    
    /**
     * 生成对比分析报告
     */
    private static void generateComparisonReport(Map<String, List<Map<String, Object>>> gaResults,
                                                Map<String, List<Map<String, Object>>> psoResults) throws Exception {
        String reportPath = RESULT_BASE_DIR + "算法对比分析报告.txt";
        PrintWriter writer = new PrintWriter(new FileWriter(reportPath));
        
        writer.println("================================================================================");
        writer.println("                     混合GA vs PSO - 算法对比分析报告");
        writer.println("================================================================================");
        writer.println("实验时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        writer.println();
        
        writer.println("【实验设计】");
        writer.println("  算法1: 混合GA（启发式初始化 + 局部搜索）");
        writer.println("  算法2: PSO（粒子群算法）");
        writer.println("  每个算例运行: " + RUNS_PER_INSTANCE + " 次");
        writer.println();
        
        writer.println("================================================================================");
        writer.println("                         详细对比结果");
        writer.println("================================================================================");
        writer.println();
        
        writer.println(String.format("%-20s | %-15s | %-15s | %-15s", 
            "算例名称", "混合GA平均值", "PSO平均值", "GA优于PSO(%)"));
        writer.println(repeatString("-", 80));
        
        // 统计胜负
        int gaWins = 0;
        int psoWins = 0;
        int ties = 0;
        double totalGAAvg = 0.0;
        double totalPSOAvg = 0.0;
        
        // 遍历所有算例
        Set<String> allInstanceNames = new HashSet<>();
        for (String name : gaResults.keySet()) allInstanceNames.add(name);
        for (String name : psoResults.keySet()) allInstanceNames.add(name);
        
        List<String> sortedNames = new ArrayList<>(allInstanceNames);
        Collections.sort(sortedNames);
        
        for (String instanceName : sortedNames) {
            if (gaResults.containsKey(instanceName) && psoResults.containsKey(instanceName)) {
                double gaAvg = (double) gaResults.get(instanceName).get(0).get("avg");
                double psoAvg = (double) psoResults.get(instanceName).get(0).get("avg");
                
                double improvement = ((psoAvg - gaAvg) / psoAvg) * 100.0;
                
                writer.println(String.format("%-20s | %-15.2f | %-15.2f | %+14.2f%%", 
                    instanceName, gaAvg, psoAvg, improvement));
                
                if (gaAvg < psoAvg) gaWins++;
                else if (gaAvg > psoAvg) psoWins++;
                else ties++;
                
                totalGAAvg += gaAvg;
                totalPSOAvg += psoAvg;
            }
        }
        
        int totalInstances = sortedNames.size();
        writer.println(repeatString("-", 80));
        writer.println(String.format("%-20s | %-15.2f | %-15.2f | %+14.2f%%", 
            "总平均", totalGAAvg / totalInstances, totalPSOAvg / totalInstances,
            ((totalPSOAvg - totalGAAvg) / totalPSOAvg * 100.0)));
        
        writer.println();
        writer.println("================================================================================");
        writer.println("                         胜负统计");
        writer.println("================================================================================");
        writer.println(String.format("混合GA 胜: %d 次 (%.1f%%)", gaWins, gaWins * 100.0 / totalInstances));
        writer.println(String.format("PSO 胜: %d 次 (%.1f%%)", psoWins, psoWins * 100.0 / totalInstances));
        writer.println(String.format("平局: %d 次 (%.1f%%)", ties, ties * 100.0 / totalInstances));
        
        writer.println();
        writer.println("================================================================================");
        writer.println("                         统计分析建议");
        writer.println("================================================================================");
        writer.println("建议进行以下统计检验：");
        writer.println("  1. 配对t检验 (Paired t-test)");
        writer.println("     - 检验两种算法在相同算例上的性能差异是否显著");
        writer.println("  2. Wilcoxon符号秩检验");
        writer.println("     - 非参数检验，适用于不满足正态分布的情况");
        writer.println("  3. 效应量分析 (Effect Size)");
        writer.println("     - Cohen's d：量化性能改进的幅度");
        writer.println();
        
        writer.println("================================================================================");
        writer.close();
        
        System.out.println("\n✅ 对比分析报告已生成: " + reportPath);
    }
    
    /**
     * 辅助方法
     */
    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    /**
     * 主函数
     */
    public static void main(String[] args) {
        System.out.println(repeatString("=", 100));
        System.out.println("                     混合GA vs PSO - 算法对比实验");
        System.out.println(repeatString("=", 100));
        System.out.println("本程序将依次运行：");
        System.out.println("  1. 混合GA算法 → 对比试验\\混合GA\\");
        System.out.println("  2. PSO算法 → 对比试验\\PSO\\");
        System.out.println("  3. 生成对比分析报告");
        System.out.println(repeatString("=", 100));
        System.out.println("\n请分别运行：");
        System.out.println("  1. ComprehensiveExperimentRunner  (运行混合GA)");
        System.out.println("  2. PSOExperimentRunner            (运行PSO)");
        System.out.println("\n两者都完成后，运行本程序生成对比分析报告。");
        System.out.println("\n或者，取消注释下面的代码自动运行两个算法。\n");
        
        // 自动运行模式（需手动取消注释）
        /*
        try {
            System.out.println("开始运行混合GA算法...");
            ComprehensiveExperimentRunner.main(args);
            
            System.out.println("\n开始运行PSO算法...");
            PSOExperimentRunner.main(args);
            
            System.out.println("\n生成对比分析报告...");
            // 这里添加对比报告生成逻辑
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
    }
}
