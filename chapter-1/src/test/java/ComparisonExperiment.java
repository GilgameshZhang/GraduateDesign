import AlgorithmFrame.machineChoice.ga.BatchGa;
import ProblemFrame.Input;
import ProblemFrame.Result;
import util.ReadDataUtil;
import util.ExperimentResultWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 对比实验：GA-TS算法对比实验
 * GA-TS: 外层GA用于机器分配优化，内层TS用于机器内次序优化
 */
public class ComparisonExperiment {
    
    /**
     * 运行单次对比实验（带可视化）
     */
    public static ExperimentResult runSingleComparison(String machinePath, String itemPath, 
                                                       int runId, String outputDir, 
                                                       boolean enableVisualization) {
        long seed = 12345L + runId;
        
        System.out.println("\n========== 运行 " + runId + " ==========");
        
        try {
            // 读取数据
            Input input = ReadDataUtil.readData(machinePath, itemPath, true);
            
            // 创建并运行GA-TS算法（使用5分钟时间限制）
            // 外层GA用于机器分配优化，内层TS用于机器内次序优化
            long timeLimitMs = 5 * 60 * 1000;  // 5分钟
            
            BatchGa batchGa = new BatchGa(
                10000,  // MAX_GEN - 设置一个很大的值，实际由时间控制
                100,    // popSize - 种群规模
                5,      // variationExchangeCount - 变异交换次数
                2,      // cloneNumOfBestIndividual - 克隆最优个体数量
                0.2,    // mutationRate - 变异率
                0.8,    // crossoverRate - 交叉率
                input,
                true,   // isRotateEnable - 是否允许旋转
                "TabuSearch",  // method - 使用禁忌搜索
                100,    // decodeMaxGen - 解码最大迭代次数
                10,     // decodeTabuSize - 禁忌表大小
                30,     // decodeMaxN - 禁忌搜索邻域大小
                timeLimitMs  // 时间限制：5分钟
            );
            
            System.out.println("运行GA-TS算法（时间限制：5分钟）...");
            long startTime = System.currentTimeMillis();
            Result result = batchGa.solve();
            long endTime = System.currentTimeMillis();
            
            if (result != null && batchGa.bestGenome != null) {
                ExperimentResult expResult = new ExperimentResult();
                expResult.runId = runId;
                expResult.cmax = batchGa.bestGenome.cMax;
                expResult.timeMs = endTime - startTime;
                expResult.algorithmName = "GA-TS";
                
                // 计算平均利用率
                expResult.avgUtilization = calculateAvgUtilization(batchGa);
                
                System.out.println("运行 " + runId + " 完成: Cmax = " + expResult.cmax + 
                                 ", 利用率 = " + String.format("%.2f%%", expResult.avgUtilization * 100) +
                                 ", 时间 = " + expResult.timeMs + "ms");
                
                // 生成完整的实验报告和可视化图表（为每次运行都生成）
                if (enableVisualization) {
                    try {
                        // 创建实验结果写入器
                        ExperimentResultWriter writer = new ExperimentResultWriter(
                            "GA-TS", runId, "ComparisonTest"
                        );
                        
                        // 写入实验基本信息
                        writer.writeExperimentInfo(
                            "GA-TS (外层GA+内层禁忌搜索)", runId, "对比实验",
                            10000, 100, 0.2, 0.8
                        );
                        
                        // 写入最终结果并生成所有图表
                        writer.writeFinalResults(
                            batchGa.bestGenome.cMax,
                            endTime - startTime,
                            batchGa.t,
                            result.getIreatorList(),
                            batchGa.bestGenome,
                            batchGa.machines
                        );
                        
                        // 写入机器负载信息
                        writer.writeMachineLoads(batchGa.bestGenome);
                        
                        // 关闭写入器
                        writer.close();
                        
                        System.out.println("✅ 实验报告和图表已生成: " + writer.getExperimentFolder());
                    } catch (Exception e) {
                        System.err.println("⚠️ 生成实验报告失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                return expResult;
            }
            
        } catch (Exception e) {
            System.err.println("运行实验失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * 计算平均利用率
     */
    private static double calculateAvgUtilization(BatchGa batchGa) {
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
    
    /**
     * 运行单次对比实验（不带可视化，兼容旧代码）
     */
    public static ExperimentResult runSingleComparison(String machinePath, String itemPath, int runId) {
        return runSingleComparison(machinePath, itemPath, runId, 
                                  "chapter-1/src/main/output/comparison", false);
    }
    
    /**
     * 运行多次实验并统计结果（带可视化）
     * 注意：为每一次运行都生成可视化图表
     */
    public static void runComparison(String machinePath, String itemPath, int numRuns, 
                                    boolean enableVisualization) {
        List<ExperimentResult> results = new ArrayList<>();
        
        System.out.println("========================================");
        System.out.println("开始对比实验");
        System.out.println("算法: GA-TS (外层GA机器分配 + 内层TS次序优化)");
        System.out.println("机器文件: " + machinePath);
        System.out.println("作业文件: " + itemPath);
        System.out.println("运行次数: " + numRuns);
        System.out.println("可视化: " + (enableVisualization ? "每次运行都生成" : "关闭"));
        System.out.println("========================================");
        
        String outputDir = "chapter-1/src/main/output/comparison";
        
        // 运行多次实验 - 为每次运行都生成可视化
        for (int i = 1; i <= numRuns; i++) {
            ExperimentResult result = runSingleComparison(
                machinePath, itemPath, i, outputDir, enableVisualization
            );
            if (result != null) {
                results.add(result);
            }
        }
        
        // 统计结果
        if (!results.isEmpty()) {
            Statistics stats = calculateStatistics(results);
            printStatistics(stats);
            
            // 保存结果到文件
            saveResults(results, stats, machinePath, itemPath);
        } else {
            System.out.println("没有有效的实验结果！");
        }
    }
    
    /**
     * 运行多次实验并统计结果（不带可视化，兼容旧代码）
     */
    public static void runComparison(String machinePath, String itemPath, int numRuns) {
        runComparison(machinePath, itemPath, numRuns, false);
    }
    
    /**
     * 计算统计信息
     */
    private static Statistics calculateStatistics(List<ExperimentResult> results) {
        Statistics stats = new Statistics();
        
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
                stats.worstResult = result;
            }
        }
        
        stats.avgCmax = sumCmax / results.size();
        stats.avgUtilization = sumUtil / results.size();
        stats.avgTimeMs = sumTime / results.size();
        stats.minCmax = minCmax;
        stats.maxCmax = maxCmax;
        
        // 计算标准差
        double sumSquaredDiff = 0.0;
        for (ExperimentResult result : results) {
            double diff = result.cmax - stats.avgCmax;
            sumSquaredDiff += diff * diff;
        }
        stats.stdDevCmax = Math.sqrt(sumSquaredDiff / results.size());
        
        return stats;
    }
    
    /**
     * 打印统计结果
     */
    private static void printStatistics(Statistics stats) {
        System.out.println("\n========================================");
        System.out.println("GA-TS算法实验统计结果");
        System.out.println("========================================");
        System.out.println(String.format("平均Cmax:      %.4f", stats.avgCmax));
        System.out.println(String.format("最小Cmax:      %.4f (运行 %d)", stats.minCmax, stats.bestResult.runId));
        System.out.println(String.format("最大Cmax:      %.4f (运行 %d)", stats.maxCmax, stats.worstResult.runId));
        System.out.println(String.format("标准差:        %.4f", stats.stdDevCmax));
        System.out.println(String.format("平均利用率:    %.2f%%", stats.avgUtilization * 100));
        System.out.println(String.format("平均时间:      %.2f ms", stats.avgTimeMs));
        System.out.println("========================================\n");
    }
    
    /**
     * 保存结果到文件
     */
    private static void saveResults(List<ExperimentResult> results, Statistics stats, 
                                   String machinePath, String itemPath) {
        String outputPath = "chapter-1/src/main/output/GATS_comparison_results.csv";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            // 写入表头
            writer.println("RunID,Algorithm,Cmax,AvgUtilization,TimeMs,MachinePath,ItemPath");
            
            // 写入每次运行的结果
            for (ExperimentResult result : results) {
                writer.println(String.format("%d,%s,%.4f,%.4f,%d,%s,%s",
                    result.runId,
                    result.algorithmName,
                    result.cmax,
                    result.avgUtilization,
                    result.timeMs,
                    machinePath,
                    itemPath
                ));
            }
            
            // 写入统计信息
            writer.println("\nStatistics");
            writer.println(String.format("Average Cmax,%.4f", stats.avgCmax));
            writer.println(String.format("Min Cmax,%.4f", stats.minCmax));
            writer.println(String.format("Max Cmax,%.4f", stats.maxCmax));
            writer.println(String.format("Std Dev,%.4f", stats.stdDevCmax));
            writer.println(String.format("Average Utilization,%.4f", stats.avgUtilization));
            writer.println(String.format("Average Time (ms),%.2f", stats.avgTimeMs));
            
            System.out.println("结果已保存到: " + outputPath);
            
        } catch (IOException e) {
            System.err.println("保存结果文件时出错: " + e.getMessage());
        }
    }
    
    /**
     * 实验结果类
     */
    static class ExperimentResult {
        int runId;
        String algorithmName;
        double cmax;
        double avgUtilization;
        long timeMs;
    }
    
    /**
     * 统计信息类
     */
    static class Statistics {
        double avgCmax;
        double minCmax;
        double maxCmax;
        double stdDevCmax;
        double avgUtilization;
        double avgTimeMs;
        ExperimentResult bestResult;
        ExperimentResult worstResult;
    }
    
    /**
     * 主函数：运行示例
     * GA-TS算法对比实验：外层GA用于机器分配优化，内层TS用于机器内次序优化
     * 
     * 注意：算法使用5分钟时间限制作为终止条件
     * 预计耗时：3次 × 5分钟 = 15分钟
     */
    public static void main(String[] args) {
        // 示例1: 小规模算例（每次运行5分钟，带可视化）
        System.out.println("===== GA-TS算法测试算例1: 2机器10作业 =====");
        System.out.println("===== 时间限制：每次运行5分钟 =====");
        runComparison(
            "chapter-1/src/main/resources/Machine/machine_2",
            "chapter-1/src/main/resources/PrintItem/printItem_10/printItem_10_01",
            3,      // 运行3次（每次5分钟）
            true    // 开启可视化（每次运行都生成完整图表）
        );
        
        // 示例2: 中等规模算例（带可视化）
        System.out.println("\n===== GA-TS算法测试算例2: 3机器20作业 =====");
        runComparison(
            "chapter-1/src/main/resources/Machine/machine_3",
            "chapter-1/src/main/resources/PrintItem/printItem_20/printItem_20_01",
            3,      // 运行3次
            true    // 开启可视化（每次运行都生成完整图表）
        );
        
        // 示例3: 大规模算例（带可视化）
        // System.out.println("\n===== GA-TS算法测试算例3: 4机器30作业 =====");
        // runComparison(
        //     "chapter-1/src/main/resources/Machine/machine_4",
        //     "chapter-1/src/main/resources/PrintItem/printItem_30/printItem_30_01",
        //     3,      // 运行3次
        //     true    // 开启可视化（每次运行都生成完整图表）
        // );
    }
}
