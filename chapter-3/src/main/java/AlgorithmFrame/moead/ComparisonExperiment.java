package AlgorithmFrame.moead;

import AlgorithmFrame.nsgaii.NSGAII;
import ProblemFrame.*;
import ProgramEntity.*;
import java.io.File;
import java.util.*;

/**
 * MOEA/D 与 NSGA-II 对比实验
 * 
 * 用于对比MOEA/D和NSGA-II在同一问题上的性能
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class ComparisonExperiment {
    
    /**
     * 运行单次对比实验
     * 
     * @param problemFile 问题文件路径
     * @param populationSize 种群大小
     * @param maxGenerations 最大代数
     * @param seed 随机种子
     */
    public static void runComparison(String problemFile, int populationSize, 
                                    int maxGenerations, long seed) {
        
        System.out.println("========================================");
        System.out.println("   MOEA/D vs NSGA-II 对比实验");
        System.out.println("========================================");
        System.out.println("问题文件: " + problemFile);
        System.out.println("种群大小: " + populationSize);
        System.out.println("最大代数: " + maxGenerations);
        System.out.println("随机种子: " + seed);
        System.out.println("========================================\n");
        
        try {
            // 加载问题
            System.out.println("加载问题数据...");
            File instanceFile = new File(problemFile);
            EnergyAwareInput input = new EnergyAwareInput(instanceFile);
            Problem problem = input.getProblemDesFromFile();
            System.out.println("工件数: " + problem.getJobCount());
            System.out.println();
            
            // 定义双目标
            List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
                new MOEvaluator.MaximumCompletionTime(),
                new MOEvaluator.TotalEnergyConsumption(problem)
            );
            
            // ==================== 运行 MOEA/D ====================
            System.out.println("\n>>> 运行 MOEA/D <<<\n");
            long moeadStartTime = System.currentTimeMillis();
            
            MOEAD moead = new MOEAD(problem, objectives);
            moead.setPopulationSize(populationSize);
            moead.setMaxGenerations(maxGenerations);
            moead.setSeed(seed);
            moead.setNeighborhoodSize(20);
            moead.setNeighborhoodSelectionProb(0.9);
            moead.setMaxReplacementSize(2);
            moead.setScalarizingFunction(MOEADOperations.ScalarizingFunction.TCHEBYCHEFF);
            
            List<MOIndividual> moeadPareto = moead.solve();
            long moeadTime = System.currentTimeMillis() - moeadStartTime;
            
            // ==================== 运行 NSGA-II ====================
            System.out.println("\n>>> 运行 NSGA-II <<<\n");
            long nsgaiiStartTime = System.currentTimeMillis();
            
            NSGAII nsgaii = new NSGAII(problem, objectives);
            nsgaii.setPopulationSize(populationSize);
            nsgaii.setMaxGenerations(maxGenerations);
            nsgaii.setSeed(seed);
            
            List<MOIndividual> nsgaiiPareto = nsgaii.solve();
            long nsgaiiTime = System.currentTimeMillis() - nsgaiiStartTime;
            
            // ==================== 对比结果 ====================
            System.out.println("\n========================================");
            System.out.println("           对比结果");
            System.out.println("========================================");
            
            System.out.println("\n1. Pareto前沿大小:");
            System.out.println("  MOEA/D:  " + moeadPareto.size());
            System.out.println("  NSGA-II: " + nsgaiiPareto.size());
            
            System.out.println("\n2. 运行时间:");
            System.out.printf("  MOEA/D:  %.2f 秒\n", moeadTime / 1000.0);
            System.out.printf("  NSGA-II: %.2f 秒\n", nsgaiiTime / 1000.0);
            
            // 计算目标范围
            if (!moeadPareto.isEmpty()) {
                double[] moeadCmaxRange = getObjectiveRange(moeadPareto, 0);
                double[] moeadEnergyRange = getObjectiveRange(moeadPareto, 1);
                
                System.out.println("\n3. MOEA/D 目标范围:");
                System.out.printf("  Cmax:   [%.2f, %.2f]\n", moeadCmaxRange[0], moeadCmaxRange[1]);
                System.out.printf("  Energy: [%.2f, %.2f]\n", moeadEnergyRange[0], moeadEnergyRange[1]);
            }
            
            if (!nsgaiiPareto.isEmpty()) {
                double[] nsgaiiCmaxRange = getObjectiveRange(nsgaiiPareto, 0);
                double[] nsgaiiEnergyRange = getObjectiveRange(nsgaiiPareto, 1);
                
                System.out.println("\n4. NSGA-II 目标范围:");
                System.out.printf("  Cmax:   [%.2f, %.2f]\n", nsgaiiCmaxRange[0], nsgaiiCmaxRange[1]);
                System.out.printf("  Energy: [%.2f, %.2f]\n", nsgaiiEnergyRange[0], nsgaiiEnergyRange[1]);
            }
            
            // 计算超体积（如果是2目标）
            if (objectives.size() == 2 && !moeadPareto.isEmpty() && !nsgaiiPareto.isEmpty()) {
                // 使用统一的参考点
                double[] referencePoint = getUnifiedReferencePoint(moeadPareto, nsgaiiPareto);
                
                double moeadHV = calculateHypervolume2D(moeadPareto, referencePoint);
                double nsgaiiHV = calculateHypervolume2D(nsgaiiPareto, referencePoint);
                
                System.out.println("\n5. 超体积指标 (Hypervolume):");
                System.out.printf("  MOEA/D:  %.2f\n", moeadHV);
                System.out.printf("  NSGA-II: %.2f\n", nsgaiiHV);
                System.out.printf("  比例:    %.2f%%\n", (moeadHV / nsgaiiHV) * 100);
            }
            
            System.out.println("========================================\n");
            
        } catch (Exception e) {
            System.err.println("对比实验失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取目标值范围
     */
    private static double[] getObjectiveRange(List<MOIndividual> front, int objectiveIndex) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        
        for (MOIndividual ind : front) {
            double value = ind.objectives[objectiveIndex];
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        
        return new double[]{min, max};
    }
    
    /**
     * 获取统一的参考点（用于公平比较超体积）
     */
    private static double[] getUnifiedReferencePoint(List<MOIndividual> front1, 
                                                     List<MOIndividual> front2) {
        int numObjectives = front1.get(0).objectives.length;
        double[] referencePoint = new double[numObjectives];
        
        for (int i = 0; i < numObjectives; i++) {
            double max1 = Double.NEGATIVE_INFINITY;
            double max2 = Double.NEGATIVE_INFINITY;
            
            for (MOIndividual ind : front1) {
                max1 = Math.max(max1, ind.objectives[i]);
            }
            
            for (MOIndividual ind : front2) {
                max2 = Math.max(max2, ind.objectives[i]);
            }
            
            // 使用两者中的最大值，再放大10%
            referencePoint[i] = Math.max(max1, max2) * 1.1;
        }
        
        return referencePoint;
    }
    
    /**
     * 计算2D超体积
     */
    private static double calculateHypervolume2D(List<MOIndividual> front, double[] referencePoint) {
        if (front.isEmpty() || front.get(0).objectives.length != 2) {
            return 0.0;
        }
        
        List<MOIndividual> sorted = new ArrayList<>(front);
        sorted.sort(Comparator.comparingDouble(ind -> ind.objectives[0]));
        
        double hypervolume = 0.0;
        double prevY = referencePoint[1];
        
        for (MOIndividual ind : sorted) {
            double x = referencePoint[0] - ind.objectives[0];
            double y = prevY - ind.objectives[1];
            
            if (x > 0 && y > 0) {
                hypervolume += x * y;
                prevY = ind.objectives[1];
            }
        }
        
        return hypervolume;
    }
    
    /**
     * 批量对比实验（多个随机种子）
     */
    public static void runMultipleComparisons(String problemFile, int populationSize,
                                             int maxGenerations, int numRuns) {
        
        System.out.println("========================================");
        System.out.println("  批量对比实验 (" + numRuns + " 次运行)");
        System.out.println("========================================\n");
        
        for (int i = 0; i < numRuns; i++) {
            System.out.println("\n>>> 第 " + (i + 1) + " 次运行 <<<");
            long seed = 12345L + i * 1000;
            
            try {
                runComparison(problemFile, populationSize, maxGenerations, seed);
                
            } catch (Exception e) {
                System.err.println("第 " + (i + 1) + " 次运行失败: " + e.getMessage());
            }
        }
        
        System.out.println("\n========================================");
        System.out.println("  批量实验完成！");
        System.out.println("========================================\n");
    }
    
    /**
     * 主函数 - 示例
     */
    public static void main(String[] args) {
        // 示例：运行单次对比实验
        String problemFile = "src/main/resources/data/test_problem.txt";
        
        runComparison(problemFile, 50, 100, 12345L);
        
        // 如果需要批量实验，取消注释：
        // runMultipleComparisons(problemFile, 50, 100, 5);
    }
}
