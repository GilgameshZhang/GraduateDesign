package AlgorithmFrame.spea2;

import AlgorithmFrame.nsgaii.NSGAII;
import ProblemFrame.*;
import ProgramEntity.*;
import java.io.File;
import java.util.*;

/**
 * SPEA2 与 NSGA-II 对比实验
 * 
 * 用于对比SPEA2和NSGA-II在同一问题上的性能
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class SPEA2ComparisonExperiment {
    
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
        System.out.println("   SPEA2 vs NSGA-II 对比实验");
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
            
            // ==================== 运行 SPEA2 ====================
            System.out.println("\n>>> 运行 SPEA2 <<<\n");
            long spea2StartTime = System.currentTimeMillis();
            
            SPEA2 spea2 = new SPEA2(problem, objectives);
            spea2.setPopulationSize(populationSize);
            spea2.setMaxGenerations(maxGenerations);
            spea2.setSeed(seed);
            
            List<MOIndividual> spea2Pareto = spea2.solve();
            long spea2Time = System.currentTimeMillis() - spea2StartTime;
            
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
            System.out.println("  SPEA2:   " + spea2Pareto.size());
            System.out.println("  NSGA-II: " + nsgaiiPareto.size());
            
            System.out.println("\n2. 运行时间:");
            System.out.printf("  SPEA2:   %.2f 秒\n", spea2Time / 1000.0);
            System.out.printf("  NSGA-II: %.2f 秒\n", nsgaiiTime / 1000.0);
            
            // 计算目标范围
            if (!spea2Pareto.isEmpty()) {
                double[] spea2CmaxRange = getObjectiveRange(spea2Pareto, 0);
                double[] spea2EnergyRange = getObjectiveRange(spea2Pareto, 1);
                
                System.out.println("\n3. SPEA2 目标范围:");
                System.out.printf("  Cmax:   [%.2f, %.2f]\n", spea2CmaxRange[0], spea2CmaxRange[1]);
                System.out.printf("  Energy: [%.2f, %.2f]\n", spea2EnergyRange[0], spea2EnergyRange[1]);
            }
            
            if (!nsgaiiPareto.isEmpty()) {
                double[] nsgaiiCmaxRange = getObjectiveRange(nsgaiiPareto, 0);
                double[] nsgaiiEnergyRange = getObjectiveRange(nsgaiiPareto, 1);
                
                System.out.println("\n4. NSGA-II 目标范围:");
                System.out.printf("  Cmax:   [%.2f, %.2f]\n", nsgaiiCmaxRange[0], nsgaiiCmaxRange[1]);
                System.out.printf("  Energy: [%.2f, %.2f]\n", nsgaiiEnergyRange[0], nsgaiiEnergyRange[1]);
            }
            
            // 计算超体积（如果是2目标）
            if (objectives.size() == 2 && !spea2Pareto.isEmpty() && !nsgaiiPareto.isEmpty()) {
                // 使用统一的参考点
                double[] referencePoint = getUnifiedReferencePoint(spea2Pareto, nsgaiiPareto);
                
                double spea2HV = calculateHypervolume2D(spea2Pareto, referencePoint);
                double nsgaiiHV = calculateHypervolume2D(nsgaiiPareto, referencePoint);
                
                System.out.println("\n5. 超体积指标 (Hypervolume):");
                System.out.printf("  SPEA2:   %.2f\n", spea2HV);
                System.out.printf("  NSGA-II: %.2f\n", nsgaiiHV);
                
                if (nsgaiiHV > 0) {
                    System.out.printf("  比例:    %.2f%%\n", (spea2HV / nsgaiiHV) * 100);
                }
                
                // 判断优胜者
                if (spea2HV > nsgaiiHV) {
                    System.out.println("  优胜者:  SPEA2 🏆");
                } else if (nsgaiiHV > spea2HV) {
                    System.out.println("  优胜者:  NSGA-II 🏆");
                } else {
                    System.out.println("  优胜者:  平局");
                }
            }
            
            // 计算C指标（覆盖度）
            if (!spea2Pareto.isEmpty() && !nsgaiiPareto.isEmpty()) {
                double c_spea2_nsgaii = calculateCoverage(spea2Pareto, nsgaiiPareto);
                double c_nsgaii_spea2 = calculateCoverage(nsgaiiPareto, spea2Pareto);
                
                System.out.println("\n6. C指标 (覆盖度):");
                System.out.printf("  C(SPEA2, NSGA-II) = %.4f\n", c_spea2_nsgaii);
                System.out.printf("  C(NSGA-II, SPEA2) = %.4f\n", c_nsgaii_spea2);
                System.out.println("  说明: C(A,B) 表示B中被A支配或等于的解的比例");
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
     * 计算C指标（覆盖度）
     * 
     * C(A, B) = |{b ∈ B | ∃a ∈ A: a ≻ b or a = b}| / |B|
     * 
     * 表示B中被A支配或等于的解的比例
     * 
     * @param frontA 前沿A
     * @param frontB 前沿B
     * @return C(A, B)
     */
    private static double calculateCoverage(List<MOIndividual> frontA, List<MOIndividual> frontB) {
        int dominatedCount = 0;
        
        for (MOIndividual b : frontB) {
            boolean isDominatedOrEqual = false;
            
            for (MOIndividual a : frontA) {
                if (a.dominates(b) || isEqual(a, b)) {
                    isDominatedOrEqual = true;
                    break;
                }
            }
            
            if (isDominatedOrEqual) {
                dominatedCount++;
            }
        }
        
        return (double) dominatedCount / frontB.size();
    }
    
    /**
     * 判断两个解在目标空间是否相等
     */
    private static boolean isEqual(MOIndividual a, MOIndividual b) {
        for (int i = 0; i < a.objectives.length; i++) {
            if (Math.abs(a.objectives[i] - b.objectives[i]) > 1e-9) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 批量对比实验（多个随机种子）
     */
    public static void runMultipleComparisons(String problemFile, int populationSize,
                                             int maxGenerations, int numRuns) {
        
        System.out.println("========================================");
        System.out.println("  批量对比实验 (" + numRuns + " 次运行)");
        System.out.println("========================================\n");
        
        // 统计数据
        List<Double> spea2HVs = new ArrayList<>();
        List<Double> nsgaiiHVs = new ArrayList<>();
        List<Integer> spea2Sizes = new ArrayList<>();
        List<Integer> nsgaiiSizes = new ArrayList<>();
        List<Double> spea2Times = new ArrayList<>();
        List<Double> nsgaiiTimes = new ArrayList<>();
        
        for (int i = 0; i < numRuns; i++) {
            System.out.println("\n>>> 第 " + (i + 1) + " 次运行 <<<");
            long seed = 12345L + i * 1000;
            
            try {
                // 加载问题
                File instanceFile = new File(problemFile);
                EnergyAwareInput input = new EnergyAwareInput(instanceFile);
                Problem problem = input.getProblemDesFromFile();
                
                List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
                    new MOEvaluator.MaximumCompletionTime(),
                    new MOEvaluator.TotalEnergyConsumption(problem)
                );
                
                // 运行SPEA2
                long spea2StartTime = System.currentTimeMillis();
                SPEA2 spea2 = new SPEA2(problem, objectives);
                spea2.setPopulationSize(populationSize);
                spea2.setMaxGenerations(maxGenerations);
                spea2.setSeed(seed);
                List<MOIndividual> spea2Pareto = spea2.solve();
                double spea2Time = (System.currentTimeMillis() - spea2StartTime) / 1000.0;
                
                // 运行NSGAII
                long nsgaiiStartTime = System.currentTimeMillis();
                NSGAII nsgaii = new NSGAII(problem, objectives);
                nsgaii.setPopulationSize(populationSize);
                nsgaii.setMaxGenerations(maxGenerations);
                nsgaii.setSeed(seed);
                List<MOIndividual> nsgaiiPareto = nsgaii.solve();
                double nsgaiiTime = (System.currentTimeMillis() - nsgaiiStartTime) / 1000.0;
                
                // 计算指标
                double[] refPoint = getUnifiedReferencePoint(spea2Pareto, nsgaiiPareto);
                double spea2HV = calculateHypervolume2D(spea2Pareto, refPoint);
                double nsgaiiHV = calculateHypervolume2D(nsgaiiPareto, refPoint);
                
                spea2HVs.add(spea2HV);
                nsgaiiHVs.add(nsgaiiHV);
                spea2Sizes.add(spea2Pareto.size());
                nsgaiiSizes.add(nsgaiiPareto.size());
                spea2Times.add(spea2Time);
                nsgaiiTimes.add(nsgaiiTime);
                
                System.out.printf("  SPEA2:   HV=%.2f, Size=%d, Time=%.2fs\n",
                    spea2HV, spea2Pareto.size(), spea2Time);
                System.out.printf("  NSGA-II: HV=%.2f, Size=%d, Time=%.2fs\n",
                    nsgaiiHV, nsgaiiPareto.size(), nsgaiiTime);
                
            } catch (Exception e) {
                System.err.println("第 " + (i + 1) + " 次运行失败: " + e.getMessage());
            }
        }
        
        // 输出统计结果
        System.out.println("\n========================================");
        System.out.println("  统计结果");
        System.out.println("========================================");
        
        System.out.println("\n超体积 (Hypervolume):");
        System.out.printf("  SPEA2:   平均=%.2f, 标准差=%.2f\n",
            mean(spea2HVs), stdDev(spea2HVs));
        System.out.printf("  NSGA-II: 平均=%.2f, 标准差=%.2f\n",
            mean(nsgaiiHVs), stdDev(nsgaiiHVs));
        
        System.out.println("\nPareto前沿大小:");
        System.out.printf("  SPEA2:   平均=%.1f, 标准差=%.1f\n",
            mean(spea2Sizes), stdDev(spea2Sizes));
        System.out.printf("  NSGA-II: 平均=%.1f, 标准差=%.1f\n",
            mean(nsgaiiSizes), stdDev(nsgaiiSizes));
        
        System.out.println("\n运行时间 (秒):");
        System.out.printf("  SPEA2:   平均=%.2f, 标准差=%.2f\n",
            mean(spea2Times), stdDev(spea2Times));
        System.out.printf("  NSGA-II: 平均=%.2f, 标准差=%.2f\n",
            mean(nsgaiiTimes), stdDev(nsgaiiTimes));
        
        System.out.println("\n========================================");
        System.out.println("  批量实验完成！");
        System.out.println("========================================\n");
    }
    
    /**
     * 计算平均值
     */
    private static double mean(List<? extends Number> values) {
        if (values.isEmpty()) return 0.0;
        double sum = 0.0;
        for (Number v : values) {
            sum += v.doubleValue();
        }
        return sum / values.size();
    }
    
    /**
     * 计算标准差
     */
    private static double stdDev(List<? extends Number> values) {
        if (values.size() < 2) return 0.0;
        double avg = mean(values);
        double sum = 0.0;
        for (Number v : values) {
            double diff = v.doubleValue() - avg;
            sum += diff * diff;
        }
        return Math.sqrt(sum / (values.size() - 1));
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
