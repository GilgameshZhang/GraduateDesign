package test;

import AlgorthmFrame.ga.GA;
import ProblemFrame.Solution;
import ProgramEntity.Input;
import ProgramEntity.Problem;

import java.io.File;

/**
 * Complete algorithm running example
 * Demonstrates how to run genetic algorithm with test instances
 */
public class RunAlgorithmExample {

    public static void main(String[] args) {
        // Set instance file path
        String instancePath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\instance\\J100\\J100P4B2D10_05.txt";

        System.out.println("====================================================");
        System.out.println(" 3D Printing Workshop Scheduling - GA Example");
        System.out.println("====================================================");

        try {
            // Step 1: Read instance
            System.out.println("\n[Step 1] Reading instance file...");
            File instanceFile = new File(instancePath);

            if (!instanceFile.exists()) {
                System.err.println("ERROR: Instance file not found!");
                System.err.println("Path: " + instancePath);
                return;
            }

            System.out.println("OK File path: " + instancePath);

            // Step 2: Parse problem
            System.out.println("\n[Step 2] Parsing problem instance...");
            Input input = new Input(instanceFile);
            Problem problem = input.getProblemDesFromFile();

            // Display problem info
            System.out.println("OK Problem scale:");
            System.out.println("  - Total machines: " + problem.getMachineCount());
            System.out.println("  - Printers: " + problem.getPrintMachineCount());
            System.out.println("  - Batch machines: " + problem.getBatchMachineCount());
            System.out.println("  - Total jobs: " + problem.getJobCount());
            System.out.println("  - Total operations: " + problem.getTotalOperationCount());

            // Step 3: Initialize GA
            System.out.println("\n[Step 3] Initializing genetic algorithm...");
            GA ga = new GA(problem);
            System.out.println("OK GA initialized");
            System.out.println("  - Population size: 50");
            System.out.println("  - Max iterations: 200");
            System.out.println("  - Crossover rate: 0.80");
            System.out.println("  - Mutation rate: 0.10");

            // Step 4: Run algorithm
            System.out.println("\n[Step 4] Running genetic algorithm...");
            System.out.println("This may take some time, please wait...\n");

            // Debug info
            System.out.println("Debug info:");
            System.out.println("  jobCount: " + problem.getJobCount());
            System.out.println("  totalOperationCount: " + problem.getTotalOperationCount());
            int[] operationCountArr = problem.getOperationCountArr();
            System.out.print("  operationCountArr: ");
            for (int i = 0; i < operationCountArr.length; i++) {
                System.out.print(operationCountArr[i] + " ");
            }
            System.out.println();
            int[] machineCountArr = problem.getMachineCountArr();
            System.out.print("  machineCountArr: ");
            for (int i = 0; i < Math.min(machineCountArr.length, 15); i++) {
                System.out.print(machineCountArr[i] + " ");
            }
            System.out.println();

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

