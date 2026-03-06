import AlgorthmFrame.randomKeyGA.RandomKeyGA;
import AlgorthmFrame.randomKeyGA.RandomKeyVisualizer;
import ProblemFrame.GAParameters;
import ProgramEntity.Input;
import ProgramEntity.Problem;
import util.java.ExperimentResultWriter;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 随机密钥遗传算法 - 对比实验运行器
 * 
 * 功能：
 * 1. 批量运行J10、J20、J50、J100算例（01-05）
 * 2. 每个算例运行10次
 * 3. 多线程并行加速
 * 4. 输出详细结果（迭代曲线、实验报告）
 * 5. 生成汇总统计表
 * 6. 与混合GA算法进行性能对比
 */
public class RandomKeyGAExperimentRunner {
    
    // 算例配置
    private static final String BASE_DIR = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\instance\\";
    private static final String RESULT_BASE_DIR = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\src\\main\\resources\\result\\对比试验\\随机密钥GA\\";
    
    // 实验配置
    private static final int RUNS_PER_INSTANCE = 10;  // 每个算例运行10次
    
    /**
     * 算例信息
     */
    static class InstanceInfo {
        String category;  // J10, J20, J50, J100
        String fullPath;  // 完整路径
        String name;      // 算例名称
        
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
     * 单次实验结果
     */
    static class RunResult {
        String instanceName;
        int runNo;
        double makespan;
        long runTime;
        String outputFolder;
        
        public RunResult(String instanceName, int runNo, double makespan, long runTime, String outputFolder) {
            this.instanceName = instanceName;
            this.runNo = runNo;
            this.makespan = makespan;
            this.runTime = runTime;
            this.outputFolder = outputFolder;
        }
    }
    
    /**
     * 运行单次实验
     */
    private static RunResult runSingleExperiment(String instancePath, String instanceName, 
                                                 int runNo, GAParameters params) throws Exception {
        // 解析算例
        File instanceFile = new File(instancePath);
        if (!instanceFile.exists()) {
            throw new FileNotFoundException("算例文件不存在: " + instancePath);
        }
        
        Input input = new Input(instanceFile);
        Problem problem = input.getProblemDesFromFile();
        
        // 创建RandomKeyGA实例
        RandomKeyGA ga = new RandomKeyGA(problem, params);
        
        // 创建结果输出器
        String folderName = instanceName + "_run" + runNo;
        String fullOutputPath = RESULT_BASE_DIR + instanceName + "\\" + folderName;
        
        ExperimentResultWriter writer = new ExperimentResultWriter(
            fullOutputPath, runNo, problem
        );
        writer.writeExperimentInfo("随机密钥GA算法", runNo, params);
        ga.setResultWriter(writer);
        
        // 运行算法
        long startTime = System.currentTimeMillis();
        RandomKeyGA.RandomKeySolution solution = ga.solve();
        long endTime = System.currentTimeMillis();
        
        // 写入迭代曲线数据
        writer.writeLine("\n========== 迭代曲线数据 ==========");
        List<Double> history = solution.makespanHistory;
        for (int i = 0; i < history.size(); i++) {
            writer.writeLine("Gen " + i + ": " + history.get(i));
        }
        
        // 关闭输出器
        writer.close();
        
        // 生成可视化（甘特图、打印布局图、详细报告）
        try {
            System.out.println(String.format("  正在生成可视化文件..."));
            RandomKeyVisualizer visualizer = new RandomKeyVisualizer(solution, problem);
            visualizer.generateAllVisualizations(fullOutputPath);
        } catch (Exception e) {
            System.err.println("  ⚠️ 可视化生成失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return new RunResult(instanceName, runNo, solution.cost, endTime - startTime, fullOutputPath);
    }
    
    /**
     * 运行单个算例的多次实验（多线程）
     */
    private static Map<String, Object> runInstanceExperiments(InstanceInfo instance, 
                                                              int threadCount,
                                                              GAParameters params) throws Exception {
        System.out.println("\n" + repeatString("=", 100));
        System.out.println("▶▶▶ 算例: " + instance.name);
        System.out.println("    路径: " + instance.fullPath);
        System.out.println("    运行次数: " + RUNS_PER_INSTANCE + " 次");
        System.out.println("    并行线程: " + threadCount);
        System.out.println(repeatString("=", 100));
        
        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // 用于收集结果
        List<Future<RunResult>> futures = new ArrayList<>();
        AtomicInteger completedCount = new AtomicInteger(0);
        
        // 提交所有任务
        for (int i = 1; i <= RUNS_PER_INSTANCE; i++) {
            final int runNo = i;
            Future<RunResult> future = executor.submit(new Callable<RunResult>() {
                @Override
                public RunResult call() throws Exception {
                    synchronized (System.out) {
                        System.out.println(String.format("  [线程-%s] 开始 %s 第 %d/%d 次运行", 
                            Thread.currentThread().getName(), instance.name, runNo, RUNS_PER_INSTANCE));
                    }
                    
                    RunResult result = runSingleExperiment(instance.fullPath, instance.name, runNo, params);
                    
                    int completed = completedCount.incrementAndGet();
                    
                    synchronized (System.out) {
                        System.out.println(String.format("  [线程-%s] ✓ %s 第 %d/%d 次完成! Makespan: %.2f, 耗时: %d ms", 
                            Thread.currentThread().getName(), instance.name, runNo, RUNS_PER_INSTANCE,
                            result.makespan, result.runTime));
                        System.out.println(String.format("  进度: %d/%d (%.1f%%)\n", 
                            completed, RUNS_PER_INSTANCE, (completed * 100.0 / RUNS_PER_INSTANCE)));
                    }
                    
                    return result;
                }
            });
            futures.add(future);
        }
        
        // 关闭线程池
        executor.shutdown();
        
        // 收集结果
        List<Double> makespans = new ArrayList<>();
        double minMakespan = Double.MAX_VALUE;
        double maxMakespan = Double.MIN_VALUE;
        double totalMakespan = 0.0;
        long totalTime = 0;
        
        for (Future<RunResult> future : futures) {
            try {
                RunResult result = future.get();
                makespans.add(result.makespan);
                minMakespan = Math.min(minMakespan, result.makespan);
                maxMakespan = Math.max(maxMakespan, result.makespan);
                totalMakespan += result.makespan;
                totalTime += result.runTime;
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
        double avgMakespan = totalMakespan / RUNS_PER_INSTANCE;
        double avgTime = totalTime / (double) RUNS_PER_INSTANCE;
        
        // 计算标准差
        double variance = 0.0;
        for (double makespan : makespans) {
            variance += Math.pow(makespan - avgMakespan, 2);
        }
        double stdDev = Math.sqrt(variance / RUNS_PER_INSTANCE);
        
        // 计算变异系数
        double cv = (stdDev / avgMakespan) * 100.0;
        
        System.out.println(repeatString("-", 100));
        System.out.println(String.format("✓ %s - 全部运行完成", instance.name));
        System.out.println(repeatString("-", 100));
        System.out.println(String.format("  平均 Makespan: %.2f", avgMakespan));
        System.out.println(String.format("  标准差: %.2f", stdDev));
        System.out.println(String.format("  变异系数: %.2f%%", cv));
        System.out.println(String.format("  最优值: %.2f", minMakespan));
        System.out.println(String.format("  最差值: %.2f", maxMakespan));
        System.out.println(String.format("  平均运行时间: %.2f 秒", avgTime / 1000.0));
        System.out.println(repeatString("-", 100));
        
        // 返回结果
        Map<String, Object> results = new HashMap<>();
        results.put("instanceName", instance.name);
        results.put("makespans", makespans);
        results.put("avg", avgMakespan);
        results.put("std", stdDev);
        results.put("cv", cv);
        results.put("min", minMakespan);
        results.put("max", maxMakespan);
        results.put("avgTime", avgTime);
        
        return results;
    }
    
    /**
     * 生成汇总统计表
     */
    private static void generateSummaryReport(List<Map<String, Object>> allResults) throws Exception {
        String summaryPath = RESULT_BASE_DIR + "实验汇总统计表.txt";
        PrintWriter writer = new PrintWriter(new FileWriter(summaryPath));
        
        writer.println("================================================================================");
        writer.println("                     随机密钥GA算法 - 综合实验汇总统计表");
        writer.println("================================================================================");
        writer.println("实验时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        writer.println("算例总数: " + allResults.size());
        writer.println("每个算例运行次数: " + RUNS_PER_INSTANCE);
        writer.println("总运行次数: " + (allResults.size() * RUNS_PER_INSTANCE));
        writer.println();
        writer.println("================================================================================");
        writer.println("                              详细结果");
        writer.println("================================================================================");
        writer.println();
        
        // 表头
        writer.println(String.format("%-25s %-12s %-12s %-10s %-10s %-10s %-12s", 
            "算例名称", "平均值", "最优值", "最差值", "标准差", "变异系数", "平均耗时(s)"));
        writer.println(repeatString("-", 100));
        
        // 数据行
        double totalAvg = 0.0;
        double totalBest = 0.0;
        
        for (Map<String, Object> result : allResults) {
            String name = (String) result.get("instanceName");
            double avg = (double) result.get("avg");
            double min = (double) result.get("min");
            double max = (double) result.get("max");
            double std = (double) result.get("std");
            double cv = (double) result.get("cv");
            double avgTime = (double) result.get("avgTime");
            
            writer.println(String.format("%-25s %-12.2f %-12.2f %-10.2f %-10.2f %-9.2f%% %-12.2f", 
                name, avg, min, max, std, cv, avgTime / 1000.0));
            
            totalAvg += avg;
            totalBest += min;
        }
        
        writer.println(repeatString("-", 100));
        writer.println(String.format("%-25s %-12.2f %-12.2f", 
            "总平均", totalAvg / allResults.size(), totalBest / allResults.size()));
        
        writer.println();
        writer.println("================================================================================");
        writer.println("                              按规模分组统计");
        writer.println("================================================================================");
        writer.println();
        
        // 按规模分组统计
        Map<String, List<Map<String, Object>>> grouped = new HashMap<>();
        for (Map<String, Object> result : allResults) {
            String name = (String) result.get("instanceName");
            // 提取规模标识（J10, J20, J50, J100）
            String category = "";
            if (name.startsWith("J100")) {
                category = "J100";
            } else if (name.startsWith("J50")) {
                category = "J50";
            } else if (name.startsWith("J20")) {
                category = "J20";
            } else if (name.startsWith("J10")) {
                category = "J10";
            }
            
            if (!category.isEmpty()) {
                grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(result);
            }
        }
        
        for (String category : Arrays.asList("J10", "J20", "J50", "J100")) {
            if (grouped.containsKey(category)) {
                List<Map<String, Object>> categoryResults = grouped.get(category);
                double categoryAvg = 0.0;
                double categoryBest = 0.0;
                
                for (Map<String, Object> result : categoryResults) {
                    categoryAvg += (double) result.get("avg");
                    categoryBest += (double) result.get("min");
                }
                
                writer.println(String.format("%s 规模: 算例数=%d, 平均Makespan=%.2f, 平均最优值=%.2f", 
                    category, categoryResults.size(), 
                    categoryAvg / categoryResults.size(), 
                    categoryBest / categoryResults.size()));
            }
        }
        
        writer.println();
        writer.println("================================================================================");
        writer.close();
        
        System.out.println("\n✅ 汇总统计表已生成: " + summaryPath);
    }
    
    /**
     * 辅助方法：重复字符串
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
        long globalStartTime = System.currentTimeMillis();
        
        System.out.println(repeatString("=", 100));
        System.out.println("                     随机密钥GA算法 - 综合对比实验");
        System.out.println(repeatString("=", 100));
        System.out.println("实验配置:");
        System.out.println("  - 算例规模: J10, J20, J50, J100");
        System.out.println("  - 每个算例运行: " + RUNS_PER_INSTANCE + " 次");
        System.out.println("  - 结果保存位置: " + RESULT_BASE_DIR);
        
        // 多线程配置
        int threadCount = Math.min(RUNS_PER_INSTANCE, Runtime.getRuntime().availableProcessors());
        System.out.println("  - 🚀 并行线程数: " + threadCount + " (CPU核心数: " + Runtime.getRuntime().availableProcessors() + ")");
        System.out.println(repeatString("=", 100));
        System.out.println("\n正在扫描算例文件...");
        
        // 创建结果目录
        File resultDir = new File(RESULT_BASE_DIR);
        if (!resultDir.exists()) {
            resultDir.mkdirs();
        }
        
        // 收集所有算例
        List<InstanceInfo> allInstances = new ArrayList<>();
        
        for (String category : Arrays.asList("J10", "J20", "J50", "J100")) {
            File dir = new File(BASE_DIR + category);
            
            if (!dir.exists() || !dir.isDirectory()) {
                System.out.println("⚠️  目录不存在: " + category);
                continue;
            }
            
            // 获取该目录下所有 .txt 文件
            File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
            
            if (files != null && files.length > 0) {
                // 按文件名排序
                Arrays.sort(files, Comparator.comparing(File::getName));
                
                for (File file : files) {
                    InstanceInfo instance = new InstanceInfo(category, file);
                    if (instance.isValid()) {
                        allInstances.add(instance);
                        System.out.println("  ✓ 找到算例: " + instance.name);
                    }
                }
            } else {
                System.out.println("⚠️  " + category + " 目录下没有找到算例文件");
            }
        }
        
        System.out.println("\n" + repeatString("=", 100));
        System.out.println("✓ 总共找到 " + allInstances.size() + " 个有效算例");
        System.out.println(repeatString("=", 100));
        System.out.println();
        
        // 默认GA参数
        GAParameters params = GAParameters.getDefaultParameters();
        
        // 运行所有算例
        List<Map<String, Object>> allResults = new ArrayList<>();
        int completedInstances = 0;
        
        try {
            for (InstanceInfo instance : allInstances) {
                try {
                    Map<String, Object> result = runInstanceExperiments(instance, threadCount, params);
                    allResults.add(result);
                    completedInstances++;
                    
                    System.out.println(String.format("\n📊 总体进度: %d/%d 个算例完成 (%.1f%%)\n", 
                        completedInstances, allInstances.size(), 
                        (completedInstances * 100.0 / allInstances.size())));
                    
                } catch (Exception e) {
                    System.err.println("❌ 算例 " + instance.name + " 执行失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // 生成汇总报告
            System.out.println("\n" + repeatString("=", 100));
            System.out.println("正在生成汇总统计表...");
            System.out.println(repeatString("=", 100));
            
            generateSummaryReport(allResults);
            
            long globalEndTime = System.currentTimeMillis();
            double totalHours = (globalEndTime - globalStartTime) / (1000.0 * 60.0 * 60.0);
            
            System.out.println("\n" + repeatString("=", 100));
            System.out.println("                    ✅ 全部实验完成！");
            System.out.println(repeatString("=", 100));
            System.out.println(String.format("  总耗时: %.2f 小时", totalHours));
            System.out.println("  完成算例数: " + completedInstances + "/" + allInstances.size());
            System.out.println("  总运行次数: " + (completedInstances * RUNS_PER_INSTANCE));
            System.out.println("  结果保存位置: " + RESULT_BASE_DIR);
            System.out.println(repeatString("=", 100));
            
        } catch (Exception e) {
            System.err.println("❌ 实验执行过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

