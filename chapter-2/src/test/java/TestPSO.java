import AlgorthmFrame.pso.PSO;
import ProblemFrame.PSOParameters;
import ProblemFrame.Solution;
import ProgramEntity.Input;
import ProgramEntity.Problem;

import java.io.File;

/**
 * PSO算法快速测试
 * 
 * 用于验证PSO算法是否正常工作
 * 运行单个算例，不生成详细报告，只输出到控制台
 */
public class TestPSO {
    
    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("                     PSO算法快速测试");
        System.out.println("================================================================================");
        
        try {
            // 选择一个小规模算例进行测试
            String testInstance = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\instance\\J10\\J10P2B1D2_01.txt";
            
            File instanceFile = new File(testInstance);
            if (!instanceFile.exists()) {
                System.err.println("❌ 测试算例不存在: " + testInstance);
                System.err.println("请修改路径为你实际的算例文件路径");
                return;
            }
            
            System.out.println("✓ 找到测试算例: " + instanceFile.getName());
            System.out.println("✓ 正在读取问题...");
            
            // 读取算例
            Input input = new Input(instanceFile);
            Problem problem = input.getProblemDesFromFile();
            
            System.out.println("✓ 问题读取成功");
            System.out.println("  - 工件数: " + problem.getJobCount());
            System.out.println("  - 打印机数: " + problem.getPrintMachineCount());
            System.out.println("  - 批处理机数: " + problem.getBatchMachineCount());
            System.out.println("  - 离散处理机数: " + (problem.getMachineCount() - problem.getPrintMachineCount() - problem.getBatchMachineCount()));
            System.out.println();
            
            // 使用默认参数
            PSOParameters params = PSOParameters.getDefaultParameters();
            System.out.println("✓ 使用默认PSO参数:");
            System.out.println(params);
            System.out.println();
            
            // 创建PSO实例（不设置resultWriter，输出到控制台）
            PSO pso = new PSO(problem, params);
            
            System.out.println("✓ 开始运行PSO算法...");
            System.out.println();
            
            // 运行算法
            long startTime = System.currentTimeMillis();
            Solution solution = pso.solve();
            long endTime = System.currentTimeMillis();
            
            // 输出结果
            System.out.println();
            System.out.println("================================================================================");
            System.out.println("                     测试完成！");
            System.out.println("================================================================================");
            System.out.println("✓ 最优 Makespan: " + String.format("%.2f", solution.cost));
            System.out.println("✓ 总运行时间: " + String.format("%.2f", (endTime - startTime) / 1000.0) + " 秒");
            System.out.println("================================================================================");
            System.out.println();
            System.out.println("🎉 PSO算法运行成功！");
            System.out.println("   现在可以运行 PSOExperimentRunner 进行完整的批量实验了。");
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
