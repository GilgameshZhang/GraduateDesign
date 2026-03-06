package AlgorithmFrame.moead;

import ProblemFrame.*;
import ProgramEntity.*;
import java.io.File;
import java.util.*;

/**
 * MOEA/D 快速测试入口
 * 
 * 用于快速测试MOEA/D算法的功能
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class QuickStart {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("    MOEA/D 快速测试");
        System.out.println("========================================\n");
        
        try {
            // 配置问题文件路径（请根据实际情况修改）
            String problemFile = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\instance\\J100\\J100P4B2D10_05.txt";
            
            // 1. 加载问题
            System.out.println("加载问题数据: " + problemFile);
            File instanceFile = new File(problemFile);
            EnergyAwareInput input = new EnergyAwareInput(instanceFile);
            Problem problem = input.getProblemDesFromFile();
            System.out.println("工件数: " + problem.getJobCount());
            System.out.println("打印机数: " + problem.getPrintMachineCount());
            System.out.println();
            
            // 2. 定义双目标：Cmax + 能耗
            List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
                new MOEvaluator.MaximumCompletionTime(),
                new MOEvaluator.TotalEnergyConsumption(problem)
            );
            
            // 3. 创建MOEA/D算法
            System.out.println("初始化MOEA/D算法...");
            MOEAD moead = new MOEAD(problem, objectives);
            
            // 配置参数（小规模测试）
            moead.setPopulationSize(50);
            moead.setMaxGenerations(100);
            moead.setMaxRunTimeMinutes(2.0);  // 2分钟超时
            moead.setSeed(12345L);
            
            // MOEA/D特定参数
            moead.setNeighborhoodSize(10);  // T=10（小规模种群用较小的邻域）
            moead.setNeighborhoodSelectionProb(0.9);
            moead.setMaxReplacementSize(2);
            moead.setScalarizingFunction(MOEADOperations.ScalarizingFunction.TCHEBYCHEFF);
            
            // 4. 运行算法
            System.out.println("开始运行算法...\n");
            List<MOIndividual> paretoFront = moead.solve();
            
            // 5. 输出结果摘要
            System.out.println("\n========================================");
            System.out.println("           结果摘要");
            System.out.println("========================================");
            System.out.println("Pareto前沿大小: " + paretoFront.size());
            
            if (!paretoFront.isEmpty()) {
                // 按Cmax排序
                List<MOIndividual> sorted = new ArrayList<>(paretoFront);
                sorted.sort(Comparator.comparingDouble(ind -> ind.objectives[0]));
                
                System.out.println("\n前5个解:");
                for (int i = 0; i < Math.min(5, sorted.size()); i++) {
                    MOIndividual ind = sorted.get(i);
                    System.out.printf("  解%d: Cmax=%.2f, Energy=%.2f, PackingQ=%.4f\n",
                        i + 1, ind.objectives[0], ind.objectives[1], ind.packingQ);
                }
                
                // 统计
                double minCmax = sorted.get(0).objectives[0];
                double maxCmax = sorted.get(sorted.size() - 1).objectives[0];
                double minEnergy = Double.POSITIVE_INFINITY;
                double maxEnergy = Double.NEGATIVE_INFINITY;
                
                for (MOIndividual ind : sorted) {
                    minEnergy = Math.min(minEnergy, ind.objectives[1]);
                    maxEnergy = Math.max(maxEnergy, ind.objectives[1]);
                }
                
                System.out.println("\n目标范围:");
                System.out.printf("  Cmax: [%.2f, %.2f]\n", minCmax, maxCmax);
                System.out.printf("  Energy: [%.2f, %.2f]\n", minEnergy, maxEnergy);
            }
            
            System.out.println("========================================\n");
            System.out.println("测试完成！");
            
        } catch (Exception e) {
            System.err.println("\n测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
