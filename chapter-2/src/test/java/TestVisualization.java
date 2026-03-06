import AlgorthmFrame.ga.GA;
import ProblemFrame.Solution;
import ProgramEntity.Input;
import ProgramEntity.Problem;
import ProgramEntity.Machine.Machine;
import java.io.File;

/**
 * 可视化功能测试类
 */
public class TestVisualization {

    public static void main(String[] args) {
        System.out.println("=== 可视化功能测试 ===\n");

        try {
            // 使用测试算例
            String instancePath = "src/main/resources/instance/J20P3B2D5_01.txt";
            File instanceFile = new File(instancePath);

            if (!instanceFile.exists()) {
                System.err.println("错误：找不到算例文件: " + instancePath);
                return;
            }

            System.out.println("加载算例文件: " + instancePath);

            // 解析算例
            Input input = new Input(instanceFile);
            Problem problem = input.getProblemDesFromFile();

            System.out.println("算例信息:");
            System.out.println("  工件数: " + problem.getJobCount());
            System.out.println("  机器数: " + problem.getMachineCount());
            System.out.println("  打印机数: " + problem.getPrintMachineCount());
            System.out.println("  批处理机数: " + problem.getBatchMachineCount());

            // 初始化GA
            System.out.println("\n初始化遗传算法...");
            GA ga = new GA(problem);

            // 运行算法（完整版本）
            System.out.println("运行遗传算法...");
            long startTime = System.currentTimeMillis();
            Solution solution = ga.solve();
            long endTime = System.currentTimeMillis();

            System.out.println("\n算法运行完成！");
            System.out.println("运行时间: " + (endTime - startTime) / 1000.0 + "秒");
            System.out.println("最优makespan: " + solution.cost);

            // 检查可视化结果
            File vizDir = new File("visualization_results");
            if (vizDir.exists() && vizDir.isDirectory()) {
                File[] files = vizDir.listFiles();
                if (files != null && files.length > 0) {
                    System.out.println("\n✅ 可视化文件生成成功:");
                    for (File file : files) {
                        System.out.println("  - " + file.getName());
                    }
                } else {
                    System.out.println("\n❌ 可视化目录为空");
                }
            } else {
                System.out.println("\n❌ 可视化目录不存在");
            }

        } catch (Exception e) {
            System.err.println("\n❌ 测试失败:");
            e.printStackTrace();
        }
    }
}
