package AlgorithmFrame.mogwo;

import ProblemFrame.*;
import ProgramEntity.*;
import java.io.File;
import java.util.*;

/**
 * MOGWO 快速启动示例
 * 
 * 演示如何快速运行MOGWO算法
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class QuickStart {
    
    public static void main(String[] args) {
        
        // ==================== 配置参数 ====================
        
        // 问题文件路径（修改为你的实际路径）
        String problemFile = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\instance\\J100\\J100P4B2D10_05.txt";
        
        // 输出目录
        String outputDir = "output/mogwo";
        
        // 算法参数
        int populationSize = 100;
        int maxGenerations = 500;
        long seed = 12345L;
        
        // ==================== 运行MOGWO ====================
        
        System.out.println("MOGWO 快速启动示例\n");
        
        try {
            // 方式1: 快速运行（默认配置）
            System.out.println(">>> 方式1: 快速运行（默认配置）\n");
            MOGWORunner.quickRun(problemFile, outputDir + "/quick", 
                populationSize, maxGenerations, seed);
            
            System.out.println("\n" + "=================================" + "\n");
            
            // 方式2: 完整运行（自定义配置）
//            System.out.println(">>> 方式2: 完整运行（自定义配置）\n");
//
//            MOGWORunner.MOGWOConfig config = new MOGWORunner.MOGWOConfig();
//            config.populationSize = 100;
//            config.archiveSize = 100;
//            config.maxGenerations = 500;
//            config.maxRunTimeMinutes = 5.0;
//            config.seed = seed;
//
//            // 目标选择
//            config.optimizeCmax = true;
//            config.optimizeEnergy = true;
//            config.optimizeTardiness = false;
//
//            // MOGWO特定参数
//            config.gridDivisions = 10;
//            config.packingQMode = MOEvaluator.PackingQMode.LAST_BATCH_MIN;
//
//            // 局部搜索（可选）
//            config.enableLocalSearch = false;
//            config.localSearchInterval = 10;
//
//            MOGWORunner.fullRun(problemFile, outputDir + "/full", config);
//
            System.out.println("\n所有实验完成！");
            
        } catch (Exception e) {
            System.err.println("运行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 示例：运行单个算例
     */
    public static void runSingleInstance() {
        String problemFile = "src/main/resources/instance/energy/J20P3B2D5_energy.txt";
        String outputDir = "output/mogwo/single";
        
        MOGWORunner.quickRun(problemFile, outputDir, 100, 500, 12345L);
    }
    
    /**
     * 示例：批量运行多个算例
     */
    public static void runMultipleInstances() {
        String[] instanceFiles = {
            "src/main/resources/instance/energy/J20P3B2D5_energy.txt",
            "src/main/resources/instance/energy/J50P5B4D10_energy.txt"
        };
        
        for (int i = 0; i < instanceFiles.length; i++) {
            String problemFile = instanceFiles[i];
            String instanceName = new File(problemFile).getName().replace(".txt", "");
            String outputDir = "output/mogwo/batch/" + instanceName;
            
            System.out.println("\n>>> 运行算例 " + (i + 1) + "/" + instanceFiles.length + ": " + instanceName + " <<<\n");
            
            MOGWORunner.quickRun(problemFile, outputDir, 100, 500, 12345L + i);
        }
        
        System.out.println("\n所有算例运行完成！");
    }
    
    /**
     * 示例：与NSGA-II对比实验
     */
    public static void comparisonExperiment() {
        String problemFile = "src/main/resources/instance/energy/J20P3B2D5_energy.txt";
        String outputBaseDir = "output/comparison";
        
        // 统一参数
        int populationSize = 100;
        int maxGenerations = 500;
        long seed = 12345L;
        
        System.out.println("========================================");
        System.out.println("  MOGWO vs NSGA-II 对比实验");
        System.out.println("========================================\n");
        
        try {
            // 加载问题
            File instanceFile = new File(problemFile);
            EnergyAwareInput input = new EnergyAwareInput(instanceFile);
            Problem problem = input.getProblemDesFromFile();
            
            // 定义目标
            List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
                new MOEvaluator.MaximumCompletionTime(),
                new MOEvaluator.TotalEnergyConsumption(problem)
            );
            
            // 1. 运行MOGWO
            System.out.println(">>> 运行 MOGWO <<<\n");
            MOGWO mogwo = new MOGWO(problem, objectives);
            mogwo.setPopulationSize(populationSize);
            mogwo.setArchiveSize(populationSize);
            mogwo.setMaxGenerations(maxGenerations);
            mogwo.setSeed(seed);
            
            long startTime = System.currentTimeMillis();
            List<MOIndividual> mogwoPF = mogwo.solve();
            long mogwoTime = System.currentTimeMillis() - startTime;
            
            // 2. 运行NSGA-II
            System.out.println("\n>>> 运行 NSGA-II <<<\n");
            AlgorithmFrame.nsgaii.NSGAII nsgaii = new AlgorithmFrame.nsgaii.NSGAII(problem, objectives);
            nsgaii.setPopulationSize(populationSize);
            nsgaii.setMaxGenerations(maxGenerations);
            nsgaii.setSeed(seed);
            
            startTime = System.currentTimeMillis();
            List<MOIndividual> nsgaiiPF = nsgaii.solve();
            long nsgaiiTime = System.currentTimeMillis() - startTime;
            
            // 3. 输出对比结果
            System.out.println("\n========================================");
            System.out.println("  对比结果");
            System.out.println("========================================");
            System.out.println("MOGWO:");
            System.out.println("  - Pareto前沿大小: " + mogwoPF.size());
            System.out.println("  - 运行时间: " + (mogwoTime / 1000.0) + " 秒");
            System.out.println("\nNSGA-II:");
            System.out.println("  - Pareto前沿大小: " + nsgaiiPF.size());
            System.out.println("  - 运行时间: " + (nsgaiiTime / 1000.0) + " 秒");
            System.out.println("========================================\n");
            
        } catch (Exception e) {
            System.err.println("对比实验失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
