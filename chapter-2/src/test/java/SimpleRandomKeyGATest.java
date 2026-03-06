import AlgorthmFrame.randomKeyGA.RandomKeyGA;
import AlgorthmFrame.randomKeyGA.RandomKeyVisualizer;
import ProblemFrame.GAParameters;
import ProgramEntity.Input;
import ProgramEntity.Problem;

import java.io.File;

/**
 * 随机密钥GA算法 - 简单测试
 * 用于快速验证算法是否正常工作
 */
public class SimpleRandomKeyGATest {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("    随机密钥GA算法 - 简单测试");
        System.out.println("========================================\n");
        
        try {
            // 1. 读取一个小算例
            String instancePath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\instance\\J100\\J100P4B2D10_05.txt";
            System.out.println("正在读取算例: " + instancePath);
            
            File instanceFile = new File(instancePath);
            if (!instanceFile.exists()) {
                System.err.println("错误：算例文件不存在！");
                System.err.println("请修改instancePath为有效的算例文件路径。");
                return;
            }
            
            Input input = new Input(instanceFile);
            Problem problem = input.getProblemDesFromFile();
            
            System.out.println("算例信息:");
            System.out.println("  - 零件数量: " + problem.getJobCount());
            System.out.println("  - 打印机数量: " + problem.getPrintMachineCount());
            System.out.println("  - 批处理机器数量: " + problem.getBatchMachineCount());
            System.out.println();
            
            // 2. 配置参数（简化配置以快速测试）
            GAParameters params = new GAParameters();
            params.popSize = 20;          // 小种群
            params.pc = 0.8;              // 交叉率
            params.pm = 0.1;              // 变异率
            params.pr = 0.3;              // 未使用（混合GA参数）
            params.maxStagnantStep = 30;  // 较小的停滞步数
            params.maxRunTime = 5.0;      // 1分钟时间限制
            
            System.out.println("算法参数:");
            System.out.println("  - 种群大小: " + params.popSize);
            System.out.println("  - 交叉率: " + params.pc);
            System.out.println("  - 变异率: " + params.pm);
            System.out.println("  - 最大停滞代数: " + params.maxStagnantStep);
            System.out.println("  - 最大运行时间: " + params.maxRunTime + "分钟");
            System.out.println();
            
            // 3. 创建并运行算法
            System.out.println("========================================");
            System.out.println("开始运行随机密钥GA算法...");
            System.out.println("========================================\n");
            
            RandomKeyGA ga = new RandomKeyGA(problem, params);
            long startTime = System.currentTimeMillis();
            RandomKeyGA.RandomKeySolution solution = ga.solve();
            long endTime = System.currentTimeMillis();
            
            // 4. 输出结果
            System.out.println("\n========================================");
            System.out.println("         测试完成！");
            System.out.println("========================================");
            System.out.println("最优Makespan: " + String.format("%.2f", solution.cost));
            System.out.println("实际运行时间: " + String.format("%.2f", (endTime - startTime) / 1000.0) + "秒");
            System.out.println("迭代次数: " + solution.makespanHistory.size());
            
            // 输出迭代曲线前10代和最后10代
            System.out.println("\n迭代曲线（前10代）:");
            for (int i = 0; i < Math.min(10, solution.makespanHistory.size()); i++) {
                System.out.println(String.format("  Gen %3d: %.2f", i, solution.makespanHistory.get(i)));
            }
            
            if (solution.makespanHistory.size() > 10) {
                System.out.println("  ...");
                int start = Math.max(10, solution.makespanHistory.size() - 10);
                System.out.println("\n迭代曲线（最后10代）:");
                for (int i = start; i < solution.makespanHistory.size(); i++) {
                    System.out.println(String.format("  Gen %3d: %.2f", i, solution.makespanHistory.get(i)));
                }
            }
            
            System.out.println("\n========================================");
            System.out.println("✓ 算法运行正常！");
            System.out.println("========================================");
            
            // 生成可视化
            System.out.println("\n正在生成可视化文件...");
            String outputDir = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\src\\main\\resources\\result\\对比试验\\随机密钥GA\\SimpleTest";
            
            try {
                RandomKeyVisualizer visualizer = new RandomKeyVisualizer(solution, problem);
                visualizer.generateAllVisualizations(outputDir);
                System.out.println("\n可视化文件已保存到: " + outputDir);
            } catch (Exception e) {
                System.err.println("可视化生成失败: " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            System.err.println("\n❌ 测试失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}

