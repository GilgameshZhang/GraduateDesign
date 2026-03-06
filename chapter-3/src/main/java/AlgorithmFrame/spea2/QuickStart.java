package AlgorithmFrame.spea2;

import ProblemFrame.*;
import ProgramEntity.*;
import java.io.File;
import java.util.*;

/**
 * SPEA2 快速开始示例
 * 
 * 提供最简单的使用示例
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class QuickStart {
    
    /**
     * 最简单的示例：双目标优化（Cmax + 能耗）
     */
    public static void simpleExample() {
        System.out.println("========================================");
        System.out.println("  SPEA2 简单示例");
        System.out.println("========================================\n");
        
        try {
            // 1. 加载问题实例
            String problemFile = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\instance\\J100\\J100P4B2D10_05.txt";
            File instanceFile = new File(problemFile);
            EnergyAwareInput input = new EnergyAwareInput(instanceFile);
            Problem problem = input.getProblemDesFromFile();
            
            // 2. 定义优化目标（Cmax + 能耗）
            List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
                new MOEvaluator.MaximumCompletionTime(),
                new MOEvaluator.TotalEnergyConsumption(problem)
            );
            
            // 3. 创建SPEA2算法实例
            SPEA2 spea2 = new SPEA2(problem, objectives);
            
            // 4. 设置参数（可选）
            spea2.setPopulationSize(50);     // 种群大小
            spea2.setMaxGenerations(100);    // 最大代数
            spea2.setSeed(12345L);           // 随机种子
            
            // 5. 运行算法
            List<MOIndividual> paretoFront = spea2.solve();
            
            // 6. 输出结果
            System.out.println("\n获得 " + paretoFront.size() + " 个Pareto最优解");
            System.out.println("\n前5个解:");
            for (int i = 0; i < Math.min(5, paretoFront.size()); i++) {
                MOIndividual ind = paretoFront.get(i);
                System.out.printf("解%d: Cmax=%.2f, Energy=%.2f\n",
                    i + 1, ind.objectives[0], ind.objectives[1]);
            }
            
        } catch (Exception e) {
            System.err.println("运行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 使用Runner快速运行
     */
    public static void useRunner() {
        System.out.println("========================================");
        System.out.println("  使用Runner快速运行SPEA2");
        System.out.println("========================================\n");
        
        String problemFile = "src/main/resources/data/test_problem.txt";
        String outputDir = "output/spea2_results";
        
        SPEA2Runner.quickRun(
            problemFile,
            outputDir,
            50,      // 种群大小
            100,     // 最大代数
            12345L   // 随机种子
        );
    }
    
    /**
     * 高级示例：启用局部搜索
     */
    public static void advancedExample() {
        System.out.println("========================================");
        System.out.println("  SPEA2 高级示例（带局部搜索）");
        System.out.println("========================================\n");
        
        try {
            // 1. 加载问题
            String problemFile = "src/main/resources/data/test_problem.txt";
            File instanceFile = new File(problemFile);
            EnergyAwareInput input = new EnergyAwareInput(instanceFile);
            Problem problem = input.getProblemDesFromFile();
            
            // 2. 定义优化目标
            List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
                new MOEvaluator.MaximumCompletionTime(),
                new MOEvaluator.TotalEnergyConsumption(problem)
            );
            
            // 3. 创建SPEA2算法
            SPEA2 spea2 = new SPEA2(problem, objectives);
            
            // 4. 设置基础参数
            spea2.setPopulationSize(100);
            spea2.setArchiveSize(100);
            spea2.setMaxGenerations(200);
            spea2.setCrossoverRate(0.9);
            spea2.setMutationRate(0.1);
            spea2.setSeed(12345L);
            
            // 5. 启用局部搜索
            spea2.enableLocalSearch(true, 10);  // 每10代执行一次
            spea2.setLocalSearchParameters(
                10,     // L: 每个精英尝试次数
                0.10,   // eta: 选择10%精英
                0.05,   // epsE: T型允许能耗上升5%
                0.02,   // epsC: E型允许Cmax上升2%
                0.005,  // improvC: T型要求Cmax下降0.5%
                0.01    // improvE: E型要求能耗下降1%
            );
            
            // 6. 运行算法
            List<MOIndividual> paretoFront = spea2.solve();
            
            // 7. 输出结果
            System.out.println("\n最终Pareto前沿大小: " + paretoFront.size());
            
        } catch (Exception e) {
            System.err.println("运行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 对比实验示例
     */
    public static void comparisonExample() {
        System.out.println("========================================");
        System.out.println("  SPEA2 vs NSGA-II 对比实验");
        System.out.println("========================================\n");
        
        String problemFile = "src/main/resources/data/test_problem.txt";
        
        // 运行单次对比
        SPEA2ComparisonExperiment.runComparison(
            problemFile,
            50,      // 种群大小
            100,     // 最大代数
            12345L   // 随机种子
        );
        
        // 如果需要批量对比（多次运行取平均），取消注释：
        // SPEA2ComparisonExperiment.runMultipleComparisons(
        //     problemFile,
        //     50,    // 种群大小
        //     100,   // 最大代数
        //     5      // 运行次数
        // );
    }
    
    /**
     * 完整配置示例
     */
    public static void fullConfigExample() {
        System.out.println("========================================");
        System.out.println("  SPEA2 完整配置示例");
        System.out.println("========================================\n");
        
        String problemFile = "src/main/resources/data/test_problem.txt";
        String outputDir = "output/spea2_full";
        
        // 创建配置
        SPEA2Runner.SPEA2Config config = new SPEA2Runner.SPEA2Config();
        
        // 设置基础参数
        config.populationSize = 100;
        config.archiveSize = 100;
        config.maxGenerations = 200;
        config.crossoverRate = 0.9;
        config.mutationRate = 0.1;
        config.maxRunTimeMinutes = 10.0;
        config.seed = 12345L;
        config.kNearest = 14;  // √(100+100) ≈ 14
        
        // 设置优化目标
        config.optimizeCmax = true;
        config.optimizeEnergy = true;
        config.optimizeTardiness = false;
        
        // 设置局部搜索
        config.enableLocalSearch = true;
        config.localSearchInterval = 10;
        config.localSearchL = 10;
        config.localSearchEta = 0.10;
        config.localSearchEpsE = 0.05;
        config.localSearchEpsC = 0.02;
        config.localSearchImprovC = 0.005;
        config.localSearchImprovE = 0.01;
        
        // 运行
        SPEA2Runner.fullRun(problemFile, outputDir, config);
    }
    
    /**
     * 主函数
     */
    public static void main(String[] args) {
        // 选择要运行的示例
        
        // 1. 最简单的示例
        simpleExample();
        
        // 2. 使用Runner
        // useRunner();
        
        // 3. 高级示例（带局部搜索）
        // advancedExample();
        
        // 4. 对比实验
        // comparisonExample();
        
        // 5. 完整配置
        // fullConfigExample();
    }
}
