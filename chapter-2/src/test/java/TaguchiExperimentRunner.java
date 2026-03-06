import AlgorthmFrame.ga.GA;
import ProblemFrame.GAParameters;
import ProblemFrame.Solution;
import ProgramEntity.Problem;
import ProgramEntity.Input;
import util.java.ExperimentResultWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 田口实验运行器
 * 用于批量运行不同参数组合的实验
 */
public class TaguchiExperimentRunner {
    
    /**
     * 运行单次实验
     * 
     * @param instancePath 算例文件路径
     * @param instanceName 算例名称（用于命名输出文件）
     * @param experimentNo 实验次数编号
     * @param params 参数配置
     * @return 最优makespan
     */
    public static double runSingleExperiment(String instancePath, String instanceName, 
                                            int experimentNo, GAParameters params) {
        try {
            // 1. 读取算例
            System.out.println("\n========================================");
            System.out.println("开始实验：" + instanceName + " 第" + experimentNo + "次");
            System.out.println("========================================");
            
            File instanceFile = new File(instancePath);
            if (!instanceFile.exists()) {
                System.err.println("❌ 算例文件不存在：" + instancePath);
                return Double.MAX_VALUE;
            }
            
            Input input = new Input(instanceFile);
            Problem problem = input.getProblemDesFromFile();
            
            // 2. 创建结果输出器（传入problem用于生成可视化图表）
            ExperimentResultWriter resultWriter = new ExperimentResultWriter(instanceName, experimentNo, problem);
            resultWriter.writeExperimentInfo(instanceName, experimentNo, params);
            
            // 3. 创建GA并设置参数
            GA ga = new GA(problem, params);
            ga.setResultWriter(resultWriter);
            
            // 4. 运行算法
            Solution solution = ga.solve();
            double makespan = solution.cost;  // Solution类中使用cost表示makespan
            
            // 5. 将最优解数据传递给输出器（用于生成可视化图表）
            if (ga.getBest() != null) {
                resultWriter.setBestSolutionData(ga.getBest(), ga.getOperationMatrix());
            }
            
            // 6. 关闭输出器
            resultWriter.close();
            
            System.out.println("实验完成！结果已保存到文件夹：" + resultWriter.getExperimentFolder());
            System.out.println("最优Makespan：" + String.format("%.2f", makespan));
            
            return makespan;
            
        } catch (IOException e) {
            System.err.println("❌ 实验运行失败：" + e.getMessage());
            e.printStackTrace();
            return Double.MAX_VALUE;
        }
    }
    
    /**
     * 运行多次实验并计算平均值
     * 
     * @param instancePath 算例文件路径
     * @param instanceName 算例名称
     * @param params 参数配置
     * @param repeatTimes 重复次数
     * @return 平均makespan
     */
    public static double runMultipleExperiments(String instancePath, String instanceName, 
                                               GAParameters params, int repeatTimes) {
        double totalMakespan = 0.0;
        double minMakespan = Double.MAX_VALUE;
        double maxMakespan = Double.MIN_VALUE;
        java.util.List<Double> allMakespans = new java.util.ArrayList<>();
        
        System.out.println("\n================================================================================");
        System.out.println("开始批量实验：" + instanceName);
        System.out.println("重复次数：" + repeatTimes);
        System.out.println("参数配置：");
        System.out.println(params.toString());
        System.out.println("================================================================================");
        
        for (int i = 1; i <= repeatTimes; i++) {
            double makespan = runSingleExperiment(instancePath, instanceName, i, params);
            allMakespans.add(makespan);
            totalMakespan += makespan;
            minMakespan = Math.min(minMakespan, makespan);
            maxMakespan = Math.max(maxMakespan, makespan);
        }
        
        double avgMakespan = totalMakespan / repeatTimes;
        
        // 计算标准差
        double variance = 0.0;
        for (double makespan : allMakespans) {
            variance += Math.pow(makespan - avgMakespan, 2);
        }
        double stdDev = Math.sqrt(variance / repeatTimes);
        
        System.out.println("\n================================================================================");
        System.out.println("批量实验完成！");
        System.out.println("================================================================================");
        System.out.println("平均Makespan: " + String.format("%.2f", avgMakespan));
        System.out.println("最小Makespan: " + String.format("%.2f", minMakespan));
        System.out.println("最大Makespan: " + String.format("%.2f", maxMakespan));
        System.out.println("标准差: " + String.format("%.2f", stdDev));
        System.out.println("变异系数(CV): " + String.format("%.2f%%", (stdDev / avgMakespan) * 100));
        System.out.println("================================================================================\n");
        
        // 生成该组实验的统计图表
        try {
            generateBatchStatisticsChart(instanceName, allMakespans, avgMakespan);
        } catch (Exception e) {
            System.err.println("⚠️ 统计图表生成失败: " + e.getMessage());
        }
        
        return avgMakespan;
    }
    
    /**
     * 生成批量实验的统计图表
     */
    private static void generateBatchStatisticsChart(String instanceName, 
                                                     java.util.List<Double> makespans,
                                                     double avgMakespan) throws Exception {
        String baseDir = "src/main/resources/result/田口实验/";
        String summaryFolder = baseDir + instanceName + "_统计汇总/";
        
        java.io.File dir = new java.io.File(summaryFolder);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 生成箱线图数据
        java.util.Map<String, java.util.List<Double>> boxData = new java.util.HashMap<>();
        boxData.put(instanceName, makespans);
        
        util.java.ChartGenerator.generateBoxPlot(
            boxData,
            summaryFolder + "makespan分布箱线图.png",
            instanceName + " - Makespan分布"
        );
        
        System.out.println("📊 统计汇总图表已生成: " + summaryFolder);
    }
    
    /**
     * 示例：运行田口L9正交表实验
     */
    public static void main(String[] args) {
        // 测试算例路径
        String instancePath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\instance\\J50\\J50P5B4D10_02.txt";
        String instanceName = "J50P5B4D10_02";
        
        // L9正交表实验设计（4因子3水平）
        // 因子: A=popSize, B=pc, C=pm, D=localSearchMaxIter
        // 注：终止条件由时间控制，不再测试maxGen参数
        int[][] l9OrthogonalArray = {
            // A    B    C    D
            {  50,  70,  10,  3},  // 实验1: 水平 1-1-1-1
            {  50,  80,  15,  5},  // 实验2: 水平 1-2-2-2
            {  50,  90,  20, 10},  // 实验3: 水平 1-3-3-3
            { 100,  70,  15, 10},  // 实验4: 水平 2-1-2-3
            { 100,  80,  20,  3},  // 实验5: 水平 2-2-3-1
            { 100,  90,  10,  5},  // 实验6: 水平 2-3-1-2
            { 150,  70,  20,  5},  // 实验7: 水平 3-1-3-2
            { 150,  80,  10, 10},  // 实验8: 水平 3-2-1-3
            { 150,  90,  15,  3}   // 实验9: 水平 3-3-2-1
        };
        
        System.out.println("================================================================================");
        System.out.println("                        田口实验 - L9正交表设计");
        System.out.println("================================================================================");
        System.out.println("测试算例: " + instanceName);
        System.out.println("实验设计: L9(3^4)");
        System.out.println("因子:");
        System.out.println("  A: 种群规模 popSize (50, 100, 150)");
        System.out.println("  B: 交叉概率 pc (0.70, 0.80, 0.90)");
        System.out.println("  C: 变异概率 pm (0.10, 0.15, 0.20)");
        System.out.println("  D: 局部搜索迭代 localSearchMaxIter (3, 5, 10)");
        System.out.println("注：算法终止由时间控制（非迭代次数）");
        System.out.println("每个参数组合运行 5 次");
        System.out.println("================================================================================\n");
        
        // 存储L9实验结果
        double[] l9Results = new double[9];
        
        // 运行9组实验
        for (int i = 0; i < l9OrthogonalArray.length; i++) {
            int[] config = l9OrthogonalArray[i];
            
            // 创建参数配置
            GAParameters params = GAParameters.createCustomParameters(
                config[0],           // popSize
                config[1] / 100.0,   // pc
                config[2] / 100.0,   // pm
                config[3]            // localSearchMaxIter
            );
            
        System.out.println("\n████████████████████████████████████████████████████████████████████████████████");
        System.out.println("                        田口实验 - 第 " + (i+1) + " 组");
        System.out.println("████████████████████████████████████████████████████████████████████████████████");
        
        // 运行5次实验并计算平均值
        double avgMakespan = runMultipleExperiments(instancePath, 
            instanceName + "_L9_" + (i+1), params, 5);
        
        System.out.println("第 " + (i+1) + " 组实验平均Makespan: " + String.format("%.2f", avgMakespan));
        l9Results[i] = avgMakespan;
    }
    
    // 生成L9实验总体对比图表
    generateL9SummaryCharts(instanceName, l9Results);
    
    System.out.println("\n================================================================================");
    System.out.println("                        所有田口实验完成！");
    System.out.println("================================================================================");
    System.out.println("结果保存路径: C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\result\\田口实验");
    System.out.println("包含:");
    System.out.println("  - 各实验独立文件夹 (包含实验报告.txt + 收敛曲线图.png)");
    System.out.println("  - L9实验总体对比图 (参数对比柱状图.png)");
    System.out.println("  - 统计汇总文件夹 (包含箱线图等)");
    System.out.println();
    System.out.println("请使用Excel或Python进行方差分析(ANOVA)和信噪比(S/N)计算");
    System.out.println("================================================================================");
}

/**
 * 生成L9实验的总体对比图表
 */
private static void generateL9SummaryCharts(String instanceName, double[] l9Results) {
    try {
        String baseDir = "src/main/resources/result/田口实验/";
        String summaryFolder = baseDir + "L9实验总体汇总/";
        
        File dir = new File(summaryFolder);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 生成参数对比柱状图
        String[] experimentNames = new String[9];
        for (int i = 0; i < 9; i++) {
            experimentNames[i] = "实验" + (i + 1);
        }
        
        util.java.ChartGenerator.generateParameterComparisonChart(
            experimentNames,
            l9Results,
            summaryFolder + "L9参数对比柱状图.png",
            "L9正交表实验 - 各组平均Makespan对比"
        );
        
        // 保存数据到文本文件
        PrintWriter writer = new PrintWriter(new FileWriter(summaryFolder + "L9实验数据汇总.txt"));
        writer.println("================================================================================");
        writer.println("                    L9正交表实验数据汇总");
        writer.println("================================================================================");
        writer.println("算例: " + instanceName);
        writer.println("实验设计: L9(3^4)");
        writer.println();
        writer.println("实验编号\t平均Makespan");
        writer.println("----------------------------------------");
        
        double minMakespan = Double.MAX_VALUE;
        double maxMakespan = Double.MIN_VALUE;
        double totalMakespan = 0.0;
        
        for (int i = 0; i < l9Results.length; i++) {
            writer.println((i+1) + "\t\t" + String.format("%.2f", l9Results[i]));
            minMakespan = Math.min(minMakespan, l9Results[i]);
            maxMakespan = Math.max(maxMakespan, l9Results[i]);
            totalMakespan += l9Results[i];
        }
        
        writer.println();
        writer.println("统计信息:");
        writer.println("  最优结果: " + String.format("%.2f", minMakespan));
        writer.println("  最差结果: " + String.format("%.2f", maxMakespan));
        writer.println("  平均值: " + String.format("%.2f", totalMakespan / l9Results.length));
        writer.println("  极差: " + String.format("%.2f", maxMakespan - minMakespan));
        writer.println();
        writer.println("================================================================================");
        writer.close();
        
        System.out.println("\n📊 L9实验总体对比图表已生成: " + summaryFolder);
        
    } catch (Exception e) {
        System.err.println("⚠️ L9总体图表生成失败: " + e.getMessage());
        e.printStackTrace();
    }
}
}
