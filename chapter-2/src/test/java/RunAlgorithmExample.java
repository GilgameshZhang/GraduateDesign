package test;

import AlgorthmFrame.ga.GA;
import ProblemFrame.Solution;
import ProgramEntity.Input;
import ProgramEntity.Problem;

import java.io.File;

/**
 * 完整的算法运行示例
 * 演示如何使用测试算例运行遗传算法
 */
public class RunAlgorithmExample {

    public static void main(String[] args) {
        // 设置算例文件路径
        String instancePath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\test_instance_small_2m_5j.txt";
        
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("         3D打印车间调度问题 - 遗传算法求解示例");
        System.out.println("═══════════════════════════════════════════════════════════\n");
        
        try {
            // ======== 第1步：读取算例 ========
            System.out.println("【第1步】读取算例文件...");
            File instanceFile = new File(instancePath);
            
            if (!instanceFile.exists()) {
                System.err.println("❌ 错误：算例文件不存在！");
                System.err.println("文件路径：" + instancePath);
                return;
            }
            
            System.out.println("✓ 文件路径：" + instancePath);
            
            // ======== 第2步：解析问题实例 ========
            System.out.println("\n【第2步】解析问题实例...");
            Input input = new Input(instanceFile);
            Problem problem = input.getProblemDesFromFile();
            
            // 显示问题信息
            System.out.println("✓ 问题规模：");
            System.out.println("  - 机器总数：" + problem.getMachineCount());
            System.out.println("  - 打印机数：" + problem.getPrintMachineCount());
            System.out.println("  - 批处理机数：" + problem.getBatchMachineCount());
            System.out.println("  - 工件总数：" + problem.getJobCount());
            System.out.println("  - 总工序数：" + problem.getTotalOperationCount());
            
            // ======== 第3步：初始化遗传算法 ========
            System.out.println("\n【第3步】初始化遗传算法...");
            GA ga = new GA(problem);
            System.out.println("✓ 遗传算法初始化完成");
            System.out.println("  - 种群规模：50");
            System.out.println("  - 最大迭代次数：200");
            System.out.println("  - 交叉概率：0.80");
            System.out.println("  - 变异概率：0.10");
            
            // ======== 第4步：运行算法求解 ========
            System.out.println("\n【第4步】开始运行遗传算法...");
            System.out.println("这可能需要一些时间，请耐心等待...\n");
            
            long startTime = System.currentTimeMillis();
            Solution solution = ga.solve();
            long endTime = System.currentTimeMillis();
            
            // ======== 第5步：输出结果 ========
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("                      求解完成！");
            System.out.println("═══════════════════════════════════════════════════════════\n");
            
            System.out.println("【求解结果】");
            if (solution != null) {
                System.out.println("✓ 求解成功！");
                // 注意：Solution类可能需要根据实际情况调整
                // 这里假设GA.solve()返回的Solution包含相关信息
                System.out.println("\n结果信息：");
                System.out.println("  （具体结果信息需要根据Solution类的实际结构获取）");
            } else {
                System.out.println("✗ 未找到可行解");
            }
            
            System.out.println("\n【性能指标】");
            double elapsedSeconds = (endTime - startTime) / 1000.0;
            System.out.println("  - 计算时间：" + String.format("%.2f", elapsedSeconds) + " 秒");
            System.out.println("  - 计算时间：" + (endTime - startTime) + " 毫秒");
            
            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("程序执行完毕！");
            System.out.println("═══════════════════════════════════════════════════════════");
            
        } catch (Exception e) {
            System.err.println("\n❌ 程序执行出错：");
            System.err.println("错误信息：" + e.getMessage());
            System.err.println("\n详细错误堆栈：");
            e.printStackTrace();
            
            System.err.println("\n💡 可能的原因：");
            System.err.println("1. 算例文件格式不正确");
            System.err.println("2. 算例数据不满足约束条件");
            System.err.println("3. 算法实现中存在bug");
            System.err.println("\n建议：");
            System.err.println("- 先使用TestInstanceValidator验证算例");
            System.err.println("- 检查错误信息中的具体位置");
            System.err.println("- 尝试使用更小的算例测试");
        }
    }
}

