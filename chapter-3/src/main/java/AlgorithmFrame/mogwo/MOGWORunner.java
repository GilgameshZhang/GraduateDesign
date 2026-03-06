package AlgorithmFrame.mogwo;

import ProblemFrame.*;
import ProgramEntity.*;
import java.io.File;
import java.util.*;

/**
 * MOGWO 算法运行器
 * 
 * 提供便捷的接口来运行MOGWO算法并导出结果
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class MOGWORunner {
    
    /**
     * 快速运行MOGWO（双目标：Cmax + 能耗）
     * 
     * @param problemFile 问题文件路径
     * @param outputDir 输出目录
     * @param populationSize 种群大小
     * @param maxGenerations 最大迭代次数
     * @param seed 随机种子
     */
    public static void quickRun(String problemFile, String outputDir, 
                               int populationSize, int maxGenerations, long seed) {
        
        System.out.println("========================================");
        System.out.println("      MOGWO 快速运行");
        System.out.println("========================================");
        System.out.println("问题文件: " + problemFile);
        System.out.println("输出目录: " + outputDir);
        System.out.println("种群大小: " + populationSize);
        System.out.println("最大迭代次数: " + maxGenerations);
        System.out.println("随机种子: " + seed);
        System.out.println("========================================\n");
        
        try {
            // 1. 加载问题
            System.out.println("加载问题数据...");
            File instanceFile = new File(problemFile);
            EnergyAwareInput input = new EnergyAwareInput(instanceFile);
            Problem problem = input.getProblemDesFromFile();
            
            // 2. 定义双目标：Cmax + 能耗
            List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
                new MOEvaluator.MaximumCompletionTime(),
                new MOEvaluator.TotalEnergyConsumption(problem)
            );
            
            // 3. 创建MOGWO算法
            MOGWO mogwo = new MOGWO(problem, objectives);
            mogwo.setPopulationSize(populationSize);
            mogwo.setArchiveSize(populationSize);  // 通常设为相同大小
            mogwo.setMaxGenerations(maxGenerations);
            mogwo.setSeed(seed);
            
            // 4. 运行算法
            List<MOIndividual> paretoFront = mogwo.solve();
            
            // 5. 导出结果
            System.out.println("\n导出结果到: " + outputDir);
            exportResults(paretoFront, outputDir, "MOGWO");
            
            System.out.println("\n运行完成！");
            
        } catch (Exception e) {
            System.err.println("运行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 完整运行MOGWO（带局部搜索）
     * 
     * @param problemFile 问题文件路径
     * @param outputDir 输出目录
     * @param config 配置参数
     */
    public static void fullRun(String problemFile, String outputDir, MOGWOConfig config) {
        
        System.out.println("========================================");
        System.out.println("      MOGWO 完整运行（带局部搜索）");
        System.out.println("========================================");
        System.out.println("问题文件: " + problemFile);
        System.out.println("输出目录: " + outputDir);
        System.out.println("配置: " + config);
        System.out.println("========================================\n");
        
        try {
            // 1. 加载问题
            System.out.println("加载问题数据...");
            File instanceFile = new File(problemFile);
            EnergyAwareInput input = new EnergyAwareInput(instanceFile);
            Problem problem = input.getProblemDesFromFile();
            
            // 2. 定义目标函数
            List<MOEvaluator.ObjectiveFunction> objectives = new ArrayList<>();
            if (config.optimizeCmax) {
                objectives.add(new MOEvaluator.MaximumCompletionTime());
            }
            if (config.optimizeEnergy) {
                objectives.add(new MOEvaluator.TotalEnergyConsumption(problem));
            }
            if (config.optimizeTardiness) {
                objectives.add(new MOEvaluator.TotalTardiness());
            }
            
            if (objectives.isEmpty()) {
                throw new IllegalArgumentException("至少需要一个优化目标");
            }
            
            // 3. 创建MOGWO算法
            MOGWO mogwo = new MOGWO(problem, objectives);
            
            // 基础参数
            mogwo.setPopulationSize(config.populationSize);
            mogwo.setArchiveSize(config.archiveSize);
            mogwo.setMaxGenerations(config.maxGenerations);
            mogwo.setMaxRunTimeMinutes(config.maxRunTimeMinutes);
            mogwo.setSeed(config.seed);
            
            // MOGWO特定参数
            mogwo.setGridDivisions(config.gridDivisions);
            mogwo.setPackingQMode(config.packingQMode);
            
            // 局部搜索
            if (config.enableLocalSearch) {
                mogwo.enableLocalSearch(true, config.localSearchInterval);
                mogwo.setLocalSearchParameters(
                    config.localSearchL,
                    config.localSearchEta,
                    config.localSearchEpsE,
                    config.localSearchEpsC,
                    config.localSearchImprovC,
                    config.localSearchImprovE
                );
            }
            
            // 4. 运行算法
            List<MOIndividual> paretoFront = mogwo.solve();
            
            // 5. 导出结果
            System.out.println("\n导出结果到: " + outputDir);
            exportResults(paretoFront, outputDir, "MOGWO");
            
            System.out.println("\n运行完成！");
            
        } catch (Exception e) {
            System.err.println("运行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * MOGWO配置类
     */
    public static class MOGWOConfig {
        // 基础参数
        public int populationSize = 100;
        public int archiveSize = 100;
        public int maxGenerations = 500;
        public double maxRunTimeMinutes = 5.0;
        public long seed = 12345L;
        
        // MOGWO特定参数
        public int gridDivisions = 10;
        public MOEvaluator.PackingQMode packingQMode = MOEvaluator.PackingQMode.LAST_BATCH_MIN;
        
        // 目标选择
        public boolean optimizeCmax = true;
        public boolean optimizeEnergy = true;
        public boolean optimizeTardiness = false;
        
        // 局部搜索
        public boolean enableLocalSearch = false;
        public int localSearchInterval = 10;
        public int localSearchL = 10;
        public double localSearchEta = 0.10;
        public double localSearchEpsE = 0.05;
        public double localSearchEpsC = 0.02;
        public double localSearchImprovC = 0.005;
        public double localSearchImprovE = 0.01;
        
        @Override
        public String toString() {
            return String.format(
                "MOGWOConfig{popSize=%d, archiveSize=%d, maxGen=%d, gridDiv=%d, LS=%s}",
                populationSize, archiveSize, maxGenerations, gridDivisions, enableLocalSearch
            );
        }
    }
    
    /**
     * 批量对比实验（不同参数配置）
     */
    public static void compareConfigurations(String problemFile, String outputBaseDir,
                                            int populationSize, int maxGenerations, long seed) {
        
        System.out.println("========================================");
        System.out.println("  MOGWO 参数对比实验");
        System.out.println("========================================\n");
        
        // 测试不同的网格划分数
        int[] gridDivisions = {5, 10, 20};
        
        for (int divisions : gridDivisions) {
            System.out.println("\n>>> 运行 gridDivisions=" + divisions + " <<<\n");
            
            MOGWOConfig config = new MOGWOConfig();
            config.populationSize = populationSize;
            config.archiveSize = populationSize;
            config.maxGenerations = maxGenerations;
            config.seed = seed;
            config.gridDivisions = divisions;
            
            String outputDir = outputBaseDir + "/grid_" + divisions;
            fullRun(problemFile, outputDir, config);
        }
        
        System.out.println("\n========================================");
        System.out.println("  对比实验完成！");
        System.out.println("========================================\n");
    }
    
    /**
     * 简化的结果导出方法
     */
    private static void exportResults(List<MOIndividual> paretoFront, String outputDir, String prefix) {
        try {
            // 创建输出目录
            File dir = new File(outputDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 导出Pareto前沿数据到CSV
            String csvPath = outputDir + "/" + prefix + "_pareto_front.csv";
            java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(csvPath));
            
            // 写入表头
            writer.write("Solution,Cmax,Energy,PackingQ,BatchCount\n");
            
            // 写入数据
            for (int i = 0; i < paretoFront.size(); i++) {
                MOIndividual ind = paretoFront.get(i);
                writer.write(String.format("%d,%.2f,%.2f,%.4f,%d\n",
                    i + 1,
                    ind.objectives[0],
                    ind.objectives.length > 1 ? ind.objectives[1] : 0.0,
                    ind.packingQ,
                    ind.batchCount));
            }
            
            writer.close();
            System.out.println("✅ Pareto前沿数据已导出: " + csvPath);
            
        } catch (Exception e) {
            System.err.println("⚠️ 导出结果失败: " + e.getMessage());
        }
    }
}
