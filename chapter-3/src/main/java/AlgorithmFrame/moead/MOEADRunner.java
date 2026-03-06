package AlgorithmFrame.moead;

import ProblemFrame.*;
import ProgramEntity.*;
import java.io.File;
import java.util.*;

/**
 * MOEA/D 算法运行器
 * 
 * 提供便捷的接口来运行MOEA/D算法并导出结果
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class MOEADRunner {
    
    /**
     * 快速运行MOEA/D（双目标：Cmax + 能耗）
     * 
     * @param problemFile 问题文件路径
     * @param outputDir 输出目录
     * @param populationSize 种群大小
     * @param maxGenerations 最大代数
     * @param seed 随机种子
     */
    public static void quickRun(String problemFile, String outputDir, 
                               int populationSize, int maxGenerations, long seed) {
        
        System.out.println("========================================");
        System.out.println("      MOEA/D 快速运行");
        System.out.println("========================================");
        System.out.println("问题文件: " + problemFile);
        System.out.println("输出目录: " + outputDir);
        System.out.println("种群大小: " + populationSize);
        System.out.println("最大代数: " + maxGenerations);
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
            
            // 3. 创建MOEA/D算法
            MOEAD moead = new MOEAD(problem, objectives);
            moead.setPopulationSize(populationSize);
            moead.setMaxGenerations(maxGenerations);
            moead.setSeed(seed);
            
            // 使用推荐的参数
            moead.setNeighborhoodSize(20);  // T=20
            moead.setNeighborhoodSelectionProb(0.9);  // delta=0.9
            moead.setMaxReplacementSize(2);  // nr=2
            moead.setScalarizingFunction(MOEADOperations.ScalarizingFunction.TCHEBYCHEFF);
            
            // 4. 运行算法
            List<MOIndividual> paretoFront = moead.solve();
            
            // 5. 导出结果
            System.out.println("\n导出结果到: " + outputDir);
            exportResults(paretoFront, outputDir, "MOEAD");
            
            System.out.println("\n运行完成！");
            
        } catch (Exception e) {
            System.err.println("运行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 完整运行MOEA/D（带局部搜索）
     * 
     * @param problemFile 问题文件路径
     * @param outputDir 输出目录
     * @param config 配置参数
     */
    public static void fullRun(String problemFile, String outputDir, MOEADConfig config) {
        
        System.out.println("========================================");
        System.out.println("      MOEA/D 完整运行（带局部搜索）");
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
            
            // 3. 创建MOEA/D算法
            MOEAD moead = new MOEAD(problem, objectives);
            
            // 基础参数
            moead.setPopulationSize(config.populationSize);
            moead.setCrossoverRate(config.crossoverRate);
            moead.setMutationRate(config.mutationRate);
            moead.setMaxGenerations(config.maxGenerations);
            moead.setMaxRunTimeMinutes(config.maxRunTimeMinutes);
            moead.setSeed(config.seed);
            
            // MOEA/D特定参数
            moead.setNeighborhoodSize(config.neighborhoodSize);
            moead.setNeighborhoodSelectionProb(config.neighborhoodSelectionProb);
            moead.setMaxReplacementSize(config.maxReplacementSize);
            moead.setScalarizingFunction(config.scalarizingFunction);
            
            // 局部搜索
            if (config.enableLocalSearch) {
                moead.enableLocalSearch(true, config.localSearchInterval);
                moead.setLocalSearchParameters(
                    config.localSearchL,
                    config.localSearchEta,
                    config.localSearchEpsE,
                    config.localSearchEpsC,
                    config.localSearchImprovC,
                    config.localSearchImprovE
                );
            }
            
            // 4. 运行算法
            List<MOIndividual> paretoFront = moead.solve();
            
            // 5. 导出结果
            System.out.println("\n导出结果到: " + outputDir);
            exportResults(paretoFront, outputDir, "MOEAD_" + config.scalarizingFunction);
            
            System.out.println("\n运行完成！");
            
        } catch (Exception e) {
            System.err.println("运行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * MOEA/D配置类
     */
    public static class MOEADConfig {
        // 基础参数
        public int populationSize = 100;
        public double crossoverRate = 0.9;
        public double mutationRate = 0.1;
        public int maxGenerations = 500;
        public double maxRunTimeMinutes = 5.0;
        public long seed = 12345L;
        
        // MOEA/D特定参数
        public int neighborhoodSize = 20;
        public double neighborhoodSelectionProb = 0.9;
        public int maxReplacementSize = 2;
        public MOEADOperations.ScalarizingFunction scalarizingFunction = 
            MOEADOperations.ScalarizingFunction.TCHEBYCHEFF;
        
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
                "MOEADConfig{popSize=%d, maxGen=%d, T=%d, delta=%.2f, nr=%d, func=%s, LS=%s}",
                populationSize, maxGenerations, neighborhoodSize, 
                neighborhoodSelectionProb, maxReplacementSize, 
                scalarizingFunction, enableLocalSearch
            );
        }
    }
    
    /**
     * 批量对比实验（不同聚合函数）
     */
    public static void compareScalarizingFunctions(String problemFile, String outputBaseDir,
                                                  int populationSize, int maxGenerations, long seed) {
        
        System.out.println("========================================");
        System.out.println("  MOEA/D 聚合函数对比实验");
        System.out.println("========================================\n");
        
        MOEADOperations.ScalarizingFunction[] functions = {
            MOEADOperations.ScalarizingFunction.TCHEBYCHEFF,
            MOEADOperations.ScalarizingFunction.WEIGHTED_SUM,
            MOEADOperations.ScalarizingFunction.PBI
        };
        
        for (MOEADOperations.ScalarizingFunction func : functions) {
            System.out.println("\n>>> 运行 " + func + " <<<\n");
            
            MOEADConfig config = new MOEADConfig();
            config.populationSize = populationSize;
            config.maxGenerations = maxGenerations;
            config.seed = seed;
            config.scalarizingFunction = func;
            
            String outputDir = outputBaseDir + "/" + func;
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
