package AlgorithmFrame.nsgaii;

import ProblemFrame.*;
import ProgramEntity.*;

import java.io.File;
import java.util.*;

/**
 * NSGA-II 快速开始示例
 * 
 * 这是一个最简化的主程序，适合快速测试和学习。
 * 只需修改算例路径即可运行。
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class QuickStart {
    
    public static void main(String[] args) {
        try {
            // ============================================
            // 1. 设置算例路径（修改这里）
            // ============================================
            String instancePath = "../chapter-2/src/main/resources/instance/J20/J20P3B2D5_01.txt";
            
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║    NSGA-II 快速开始                    ║");
            System.out.println("╚════════════════════════════════════════╝\n");
            System.out.println("算例: " + instancePath + "\n");
            
            // ============================================
            // 2. 加载算例
            // ============================================
            System.out.println("【1】加载算例...");
            File file = new File(instancePath);
            if (!file.exists()) {
                System.err.println("❌ 文件不存在: " + instancePath);
                return;
            }
            
            EnergyAwareInput input = new EnergyAwareInput(file);
            Problem problem = input.getProblemDesFromFile();
            System.out.println("✅ 加载成功: " + problem.getJobCount() + "个工件, " + 
                             problem.getMachineCount() + "台机器\n");
            
            // ============================================
            // 3. 配置目标函数
            // ============================================
            System.out.println("【2】配置目标函数...");
            List<MOEvaluator.ObjectiveFunction> objectives = new ArrayList<>();
            objectives.add(new MOEvaluator.MaximumCompletionTime());
            objectives.add(new MOEvaluator.TotalEnergyConsumption(problem, true, false));
            System.out.println("✅ 目标1: Cmax");
            System.out.println("✅ 目标2: Energy\n");
            
            // ============================================
            // 4. 创建并配置NSGA-II
            // ============================================
            System.out.println("【3】配置NSGA-II...");
            NSGAII nsgaii = new NSGAII(problem, objectives);
            
            // 快速测试参数（小规模）
            nsgaii.setPopulationSize(50);      // 种群规模
            nsgaii.setMaxGenerations(100);     // 最大代数
            nsgaii.setCrossoverRate(0.9);      // 交叉率
            nsgaii.setMutationRate(0.1);       // 变异率
            nsgaii.setMaxRunTimeMinutes(5.0);  // 最大5分钟
            
            System.out.println("✅ 参数配置完成\n");
            
            // ============================================
            // 5. 运行优化
            // ============================================
            System.out.println("【4】运行优化...\n");
            System.out.println("════════════════════════════════════════");
            long startTime = System.currentTimeMillis();
            
            List<MOIndividual> paretoFront = nsgaii.solve();
            
            long endTime = System.currentTimeMillis();
            double runtime = (endTime - startTime) / 1000.0;
            System.out.println("════════════════════════════════════════\n");
            System.out.println("✅ 优化完成! 用时: " + String.format("%.2f", runtime) + " 秒\n");
            
            // ============================================
            // 6. 显示结果
            // ============================================
            System.out.println("【5】Pareto前沿（前10个解）\n");
            displayResults(paretoFront, 10);
            
            System.out.println("\n════════════════════════════════════════");
            System.out.println("  完成！Pareto前沿规模: " + paretoFront.size());
            System.out.println("════════════════════════════════════════");
            
        } catch (Exception e) {
            System.err.println("\n❌ 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 显示结果（简化版）
     */
    private static void displayResults(List<MOIndividual> paretoFront, int maxDisplay) {
        if (paretoFront.isEmpty()) {
            System.out.println("  无解");
            return;
        }
        
        // 按Cmax排序
        List<MOIndividual> sorted = new ArrayList<>(paretoFront);
        sorted.sort((a, b) -> Double.compare(a.objectives[0], b.objectives[0]));
        
        // 表头
        System.out.println(String.format("%-6s %-12s %-14s %-12s",
            "序号", "Cmax", "能耗(kWh)", "PackingQ"));
        System.out.println("──────────────────────────────────────────────");
        
        // 显示解
        int count = Math.min(maxDisplay, sorted.size());
        for (int i = 0; i < count; i++) {
            MOIndividual ind = sorted.get(i);
            System.out.println(String.format("%-6d %-12.2f %-14.2f %-12.4f",
                i + 1,
                ind.objectives[0],
                ind.objectives[1],
                ind.packingQ
            ));
        }
        
        if (sorted.size() > maxDisplay) {
            System.out.println("... 还有 " + (sorted.size() - maxDisplay) + " 个解");
        }
    }
}

