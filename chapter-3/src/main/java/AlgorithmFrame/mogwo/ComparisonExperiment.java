package AlgorithmFrame.mogwo;

import AlgorithmFrame.nsgaii.NSGAII;
import AlgorithmFrame.spea2.SPEA2;
import AlgorithmFrame.moead.MOEAD;
import ProblemFrame.*;
import ProgramEntity.*;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;

/**
 * MOGWO与其他多目标算法的对比实验
 * 
 * 对比算法：
 * - NSGA-II
 * - SPEA2
 * - MOEA/D
 * - MOGWO
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class ComparisonExperiment {
    
    /**
     * 运行完整的对比实验
     * 
     * @param problemFile 问题文件路径
     * @param outputDir 输出目录
     * @param config 实验配置
     */
    public static void runComparison(String problemFile, String outputDir, ExperimentConfig config) {
        
        System.out.println("╔" + "==========================================================" + "╗");
        System.out.println("║" + "                " + "多目标算法对比实验" + "                        " + "║");
        System.out.println("╚" + "═=============================================================" + "╝");
        System.out.println();
        System.out.println("问题文件: " + problemFile);
        System.out.println("输出目录: " + outputDir);
        System.out.println("重复次数: " + config.numRuns);
        System.out.println("种群大小: " + config.populationSize);
        System.out.println("最大代数: " + config.maxGenerations);
        System.out.println();
        
        try {
            // 加载问题
            File instanceFile = new File(problemFile);
            EnergyAwareInput input = new EnergyAwareInput(instanceFile);
            Problem problem = input.getProblemDesFromFile();
            
            // 定义优化目标
            List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
                new MOEvaluator.MaximumCompletionTime(),
                new MOEvaluator.TotalEnergyConsumption(problem)
            );
            
            // 创建输出目录
            File dir = new File(outputDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 存储结果
            Map<String, List<ExperimentResult>> allResults = new LinkedHashMap<>();
            
            // 对于每个算法，运行多次
            String[] algorithms = {"MOGWO", "NSGA-II", "SPEA2", "MOEA/D"};
            
            for (String algorithm : algorithms) {
                System.out.println("─---------------------------------------");
                System.out.println("运行算法: " + algorithm);
                System.out.println("─------------------------------------------");
                
                List<ExperimentResult> results = new ArrayList<>();
                
                for (int run = 0; run < config.numRuns; run++) {
                    System.out.printf("\n[%s] 第 %d/%d 次运行...\n", 
                        algorithm, run + 1, config.numRuns);
                    
                    long seed = config.baseSeed + run;
                    ExperimentResult result = runSingleAlgorithm(
                        algorithm, problem, objectives, config, seed
                    );
                    
                    results.add(result);
                    
                    System.out.printf("  完成: PF大小=%d, 时间=%.2fs, HV=%.4f\n",
                        result.paretoFrontSize, result.runTime, result.hypervolume);
                }
                
                allResults.put(algorithm, results);
            }
            
            // 输出统计结果
            System.out.println("\n" + "═===============================================");
            System.out.println("统计结果");
            System.out.println("═=======================================================");
            printStatistics(allResults);
            
            // 导出结果到CSV
            exportResults(allResults, outputDir);
            
            System.out.println("\n实验完成！结果已保存到: " + outputDir);
            
        } catch (Exception e) {
            System.err.println("实验失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 运行单个算法
     */
    private static ExperimentResult runSingleAlgorithm(
        String algorithm, Problem problem, 
        List<MOEvaluator.ObjectiveFunction> objectives,
        ExperimentConfig config, long seed) {
        
        long startTime = System.currentTimeMillis();
        List<MOIndividual> paretoFront = null;
        
        try {
            switch (algorithm) {
                case "MOGWO":
                    paretoFront = runMOGWO(problem, objectives, config, seed);
                    break;
                    
                case "NSGA-II":
                    paretoFront = runNSGAII(problem, objectives, config, seed);
                    break;
                    
                case "SPEA2":
                    paretoFront = runSPEA2(problem, objectives, config, seed);
                    break;
                    
                case "MOEA/D":
                    paretoFront = runMOEAD(problem, objectives, config, seed);
                    break;
                    
                default:
                    throw new IllegalArgumentException("未知算法: " + algorithm);
            }
        } catch (Exception e) {
            System.err.println("  运行失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        long endTime = System.currentTimeMillis();
        double runTime = (endTime - startTime) / 1000.0;
        
        // 计算指标
        ExperimentResult result = new ExperimentResult();
        result.algorithm = algorithm;
        result.paretoFrontSize = (paretoFront != null) ? paretoFront.size() : 0;
        result.runTime = runTime;
        
        if (paretoFront != null && !paretoFront.isEmpty()) {
            result.hypervolume = calculateHypervolume(paretoFront, objectives.size());
            result.spacing = calculateSpacing(paretoFront);
            result.minCmax = getMinObjective(paretoFront, 0);
            result.minEnergy = getMinObjective(paretoFront, 1);
        }
        
        return result;
    }
    
    /**
     * 运行MOGWO
     */
    private static List<MOIndividual> runMOGWO(Problem problem, 
        List<MOEvaluator.ObjectiveFunction> objectives,
        ExperimentConfig config, long seed) {
        
        MOGWO mogwo = new MOGWO(problem, objectives);
        mogwo.setPopulationSize(config.populationSize);
        mogwo.setArchiveSize(config.populationSize);
        mogwo.setMaxGenerations(config.maxGenerations);
        mogwo.setSeed(seed);
        mogwo.setGridDivisions(10);
        
        return mogwo.solve();
    }
    
    /**
     * 运行NSGA-II
     */
    private static List<MOIndividual> runNSGAII(Problem problem, 
        List<MOEvaluator.ObjectiveFunction> objectives,
        ExperimentConfig config, long seed) {
        
        NSGAII nsgaii = new NSGAII(problem, objectives);
        nsgaii.setPopulationSize(config.populationSize);
        nsgaii.setMaxGenerations(config.maxGenerations);
        nsgaii.setSeed(seed);
        
        return nsgaii.solve();
    }
    
    /**
     * 运行SPEA2
     */
    private static List<MOIndividual> runSPEA2(Problem problem, 
        List<MOEvaluator.ObjectiveFunction> objectives,
        ExperimentConfig config, long seed) {
        
        SPEA2 spea2 = new SPEA2(problem, objectives);
        spea2.setPopulationSize(config.populationSize);
        spea2.setArchiveSize(config.populationSize);
        spea2.setMaxGenerations(config.maxGenerations);
        spea2.setSeed(seed);
        
        return spea2.solve();
    }
    
    /**
     * 运行MOEA/D
     */
    private static List<MOIndividual> runMOEAD(Problem problem, 
        List<MOEvaluator.ObjectiveFunction> objectives,
        ExperimentConfig config, long seed) {
        
        MOEAD moead = new MOEAD(problem, objectives);
        moead.setPopulationSize(config.populationSize);
        moead.setMaxGenerations(config.maxGenerations);
        moead.setSeed(seed);
        moead.setNeighborhoodSize(20);
        
        return moead.solve();
    }
    
    /**
     * 打印统计结果
     */
    private static void printStatistics(Map<String, List<ExperimentResult>> allResults) {
        System.out.println();
        System.out.printf("%-10s | %10s | %10s | %10s | %10s | %10s\n",
            "算法", "PF大小", "运行时间(s)", "超体积", "间距", "最小Cmax");
        System.out.println("--------------------------------------------");
        
        for (Map.Entry<String, List<ExperimentResult>> entry : allResults.entrySet()) {
            String algorithm = entry.getKey();
            List<ExperimentResult> results = entry.getValue();
            
            // 计算平均值和标准差
            double avgPFSize = results.stream().mapToDouble(r -> r.paretoFrontSize).average().orElse(0);
            double avgTime = results.stream().mapToDouble(r -> r.runTime).average().orElse(0);
            double avgHV = results.stream().mapToDouble(r -> r.hypervolume).average().orElse(0);
            double avgSpacing = results.stream().mapToDouble(r -> r.spacing).average().orElse(0);
            double avgMinCmax = results.stream().mapToDouble(r -> r.minCmax).average().orElse(0);
            
            double stdPFSize = calculateStd(results.stream().mapToDouble(r -> r.paretoFrontSize).toArray());
            double stdTime = calculateStd(results.stream().mapToDouble(r -> r.runTime).toArray());
            double stdHV = calculateStd(results.stream().mapToDouble(r -> r.hypervolume).toArray());
            
            System.out.printf("%-10s | %6.1f±%-3.1f | %6.2f±%-3.2f | %6.4f±%-4.4f | %10.4f | %10.2f\n",
                algorithm, avgPFSize, stdPFSize, avgTime, stdTime, avgHV, stdHV, avgSpacing, avgMinCmax);
        }
        
        System.out.println();
    }
    
    /**
     * 导出结果到CSV
     */
    private static void exportResults(Map<String, List<ExperimentResult>> allResults, String outputDir) {
        try {
            String csvPath = outputDir + "/comparison_results.csv";
            BufferedWriter writer = new BufferedWriter(new FileWriter(csvPath));
            
            // 写入表头
            writer.write("Algorithm,Run,ParetoFrontSize,RunTime,Hypervolume,Spacing,MinCmax,MinEnergy\n");
            
            // 写入数据
            for (Map.Entry<String, List<ExperimentResult>> entry : allResults.entrySet()) {
                String algorithm = entry.getKey();
                List<ExperimentResult> results = entry.getValue();
                
                for (int i = 0; i < results.size(); i++) {
                    ExperimentResult r = results.get(i);
                    writer.write(String.format("%s,%d,%d,%.4f,%.6f,%.6f,%.2f,%.2f\n",
                        algorithm, i + 1, r.paretoFrontSize, r.runTime, 
                        r.hypervolume, r.spacing, r.minCmax, r.minEnergy));
                }
            }
            
            writer.close();
            System.out.println("✅ 对比结果已导出: " + csvPath);
            
        } catch (Exception e) {
            System.err.println("⚠️ 导出结果失败: " + e.getMessage());
        }
    }
    
    /**
     * 计算超体积（简化版）
     */
    private static double calculateHypervolume(List<MOIndividual> paretoFront, int numObjectives) {
        if (paretoFront.isEmpty()) return 0.0;
        
        // 简化计算：归一化后的面积/体积
        double[] maxObj = new double[numObjectives];
        double[] minObj = new double[numObjectives];
        Arrays.fill(maxObj, Double.MIN_VALUE);
        Arrays.fill(minObj, Double.MAX_VALUE);
        
        // 找出最大最小值
        for (MOIndividual ind : paretoFront) {
            for (int i = 0; i < numObjectives; i++) {
                maxObj[i] = Math.max(maxObj[i], ind.objectives[i]);
                minObj[i] = Math.min(minObj[i], ind.objectives[i]);
            }
        }
        
        // 计算参考点（最差点）
        double[] refPoint = new double[numObjectives];
        for (int i = 0; i < numObjectives; i++) {
            refPoint[i] = maxObj[i] * 1.1;  // 稍微超出最大值
        }
        
        // 简化HV计算（仅对双目标有效）
        if (numObjectives == 2) {
            // 按第一个目标排序
            List<MOIndividual> sorted = new ArrayList<>(paretoFront);
            sorted.sort(Comparator.comparingDouble(ind -> ind.objectives[0]));
            
            double hv = 0.0;
            double prevY = refPoint[1];
            
            for (MOIndividual ind : sorted) {
                double width = refPoint[0] - ind.objectives[0];
                double height = prevY - ind.objectives[1];
                hv += width * height;
                prevY = ind.objectives[1];
            }
            
            return hv;
        }
        
        return 0.0;  // 暂不支持三目标以上
    }
    
    /**
     * 计算间距指标（Spacing）
     */
    private static double calculateSpacing(List<MOIndividual> paretoFront) {
        if (paretoFront.size() < 2) return 0.0;
        
        int n = paretoFront.size();
        double[] distances = new double[n];
        
        // 计算每个解到最近邻的距离
        for (int i = 0; i < n; i++) {
            double minDist = Double.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    double dist = euclideanDistance(
                        paretoFront.get(i).objectives,
                        paretoFront.get(j).objectives
                    );
                    minDist = Math.min(minDist, dist);
                }
            }
            distances[i] = minDist;
        }
        
        // 计算平均距离
        double avgDist = Arrays.stream(distances).average().orElse(0.0);
        
        // 计算标准差
        double variance = 0.0;
        for (double dist : distances) {
            variance += Math.pow(dist - avgDist, 2);
        }
        variance /= n;
        
        return Math.sqrt(variance);
    }
    
    /**
     * 计算欧几里得距离
     */
    private static double euclideanDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }
    
    /**
     * 获取指定目标的最小值
     */
    private static double getMinObjective(List<MOIndividual> paretoFront, int objIndex) {
        return paretoFront.stream()
            .mapToDouble(ind -> ind.objectives[objIndex])
            .min()
            .orElse(Double.MAX_VALUE);
    }
    
    /**
     * 计算标准差
     */
    private static double calculateStd(double[] values) {
        if (values.length < 2) return 0.0;
        
        double mean = Arrays.stream(values).average().orElse(0.0);
        double variance = 0.0;
        
        for (double value : values) {
            variance += Math.pow(value - mean, 2);
        }
        variance /= values.length;
        
        return Math.sqrt(variance);
    }
    
    // ==================== 配置类 ====================
    
    /**
     * 实验配置
     */
    public static class ExperimentConfig {
        public int numRuns = 10;              // 重复运行次数
        public int populationSize = 100;      // 种群大小
        public int maxGenerations = 500;      // 最大代数
        public long baseSeed = 12345L;        // 基础随机种子
    }
    
    /**
     * 实验结果
     */
    private static class ExperimentResult {
        String algorithm;
        int paretoFrontSize;
        double runTime;
        double hypervolume;
        double spacing;
        double minCmax;
        double minEnergy;
    }
    
    // ==================== 主函数（测试用） ====================
    
    public static void main(String[] args) {
        String problemFile = "src/main/resources/instance/energy/J20P3B2D5_energy.txt";
        String outputDir = "output/comparison";
        
        ExperimentConfig config = new ExperimentConfig();
        config.numRuns = 5;              // 每个算法运行5次
        config.populationSize = 100;
        config.maxGenerations = 300;     // 为了快速测试，减少代数
        
        runComparison(problemFile, outputDir, config);
    }
}
