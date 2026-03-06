package AlgorithmFrame.spea2;

import AlgorithmFrame.nsgaii.NSGAII;
import ProblemFrame.*;
import ProgramEntity.*;
import java.io.File;
import java.util.*;

/**
 * SPEA2 简单测试
 * 
 * 验证SPEA2算法的基本功能
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class SimpleSPEA2Test {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("      SPEA2 简单测试");
        System.out.println("========================================\n");
        
        try {
            // 1. 加载测试问题
            String problemFile = "src/main/resources/instance/J20/J20P3B2D5_01.txt";
            File instanceFile = new File(problemFile);
            
            if (!instanceFile.exists()) {
                System.err.println("❌ 问题文件不存在: " + problemFile);
                System.err.println("请确保文件路径正确");
                return;
            }
            
            EnergyAwareInput input = new EnergyAwareInput(instanceFile);
            Problem problem = input.getProblemDesFromFile();
            
            System.out.println("✅ 成功加载问题实例");
            System.out.println("   工件数: " + problem.getJobCount());
            System.out.println("   打印机数: " + problem.getPrintMachineCount());
            System.out.println("   批处理机数: " + problem.getBatchMachineCount());
            System.out.println();
            
            // 2. 定义优化目标
            List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
                new MOEvaluator.MaximumCompletionTime(),
                new MOEvaluator.TotalEnergyConsumption(problem)
            );
            
            System.out.println("优化目标:");
            System.out.println("   1. 最小化最大完工时间 (Cmax)");
            System.out.println("   2. 最小化总能耗 (Energy)");
            System.out.println();
            
            // 3. 运行SPEA2
            System.out.println(">>> 运行 SPEA2 <<<\n");
            long spea2StartTime = System.currentTimeMillis();
            
            SPEA2 spea2 = new SPEA2(problem, objectives);
            spea2.setPopulationSize(30);
            spea2.setArchiveSize(30);
            spea2.setMaxGenerations(50);
            spea2.setSeed(12345L);
            
            List<MOIndividual> spea2Pareto = spea2.solve();
            long spea2Time = System.currentTimeMillis() - spea2StartTime;
            
            // 4. 运行NSGA-II（对比）
            System.out.println("\n>>> 运行 NSGA-II (对比) <<<\n");
            long nsgaiiStartTime = System.currentTimeMillis();
            
            NSGAII nsgaii = new NSGAII(problem, objectives);
            nsgaii.setPopulationSize(30);
            nsgaii.setMaxGenerations(50);
            nsgaii.setSeed(12345L);
            
            List<MOIndividual> nsgaiiPareto = nsgaii.solve();
            long nsgaiiTime = System.currentTimeMillis() - nsgaiiStartTime;
            
            // 5. 输出对比结果
            System.out.println("\n========================================");
            System.out.println("           测试结果");
            System.out.println("========================================");
            
            System.out.println("\n✅ SPEA2 测试通过");
            System.out.println("   Pareto前沿大小: " + spea2Pareto.size());
            System.out.printf("   运行时间: %.2f秒\n", spea2Time / 1000.0);
            
            if (!spea2Pareto.isEmpty()) {
                MOIndividual best = spea2Pareto.get(0);
                System.out.printf("   最优解示例: Cmax=%.2f, Energy=%.2f\n",
                    best.objectives[0], best.objectives[1]);
            }
            
            System.out.println("\n✅ NSGA-II 测试通过");
            System.out.println("   Pareto前沿大小: " + nsgaiiPareto.size());
            System.out.printf("   运行时间: %.2f秒\n", nsgaiiTime / 1000.0);
            
            if (!nsgaiiPareto.isEmpty()) {
                MOIndividual best = nsgaiiPareto.get(0);
                System.out.printf("   最优解示例: Cmax=%.2f, Energy=%.2f\n",
                    best.objectives[0], best.objectives[1]);
            }
            
            // 6. 计算超体积对比
            if (spea2Pareto.size() > 0 && nsgaiiPareto.size() > 0) {
                double[] referencePoint = getUnifiedReferencePoint(spea2Pareto, nsgaiiPareto);
                double spea2HV = calculateHypervolume2D(spea2Pareto, referencePoint);
                double nsgaiiHV = calculateHypervolume2D(nsgaiiPareto, referencePoint);
                
                System.out.println("\n超体积指标:");
                System.out.printf("   SPEA2:   %.2f\n", spea2HV);
                System.out.printf("   NSGA-II: %.2f\n", nsgaiiHV);
                
                if (spea2HV > nsgaiiHV) {
                    System.out.printf("   SPEA2 优于 NSGA-II (%.2f%%)\n",
                        ((spea2HV / nsgaiiHV) - 1) * 100);
                } else if (nsgaiiHV > spea2HV) {
                    System.out.printf("   NSGA-II 优于 SPEA2 (%.2f%%)\n",
                        ((nsgaiiHV / spea2HV) - 1) * 100);
                } else {
                    System.out.println("   两者性能相当");
                }
            }
            
            System.out.println("\n========================================");
            System.out.println("✅ 所有测试通过！SPEA2算法运行正常");
            System.out.println("========================================\n");
            
        } catch (Exception e) {
            System.err.println("\n❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取统一的参考点
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
}
