package test;

import AlgorthmFrame.ga.GA;
import ProgramEntity.Input;
import ProgramEntity.Problem;

import java.io.File;

/**
 * 调试测试类
 * 用于输出详细的调试信息
 */
public class DebugTest {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("         调试测试 - 查看详细错误信息");
        System.out.println("═══════════════════════════════════════════════════════════\n");

        try {
            // 使用最简单的算例
            String instancePath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\test_instance_small_2m_5j.txt";

            System.out.println("读取算例: test_instance_small_2m_5j.txt");
            File instanceFile = new File(instancePath);

            if (!instanceFile.exists()) {
                System.err.println("错误：算例文件不存在！");
                return;
            }

            // 读取算例
            Input input = new Input(instanceFile);
            Problem problem = input.getProblemDesFromFile();

            System.out.println("问题规模:");
            System.out.println("  机器数: " + problem.getMachineCount());
            System.out.println("  工件数: " + problem.getJobCount());
            System.out.println("  总工序数: " + problem.getTotalOperationCount());

            // 检查machineCountArr
            int[] machineCountArr = problem.getMachineCountArr();
            System.out.println("machineCountArr长度: " + machineCountArr.length);
            System.out.print("machineCountArr内容: ");
            for (int i = 0; i < Math.min(machineCountArr.length, 20); i++) {
                System.out.print(machineCountArr[i] + " ");
            }
            System.out.println();

            // 检查operationToIndex
            int[][] operationToIndex = problem.getOperationToIndex();
            System.out.println("operationToIndex:");
            for (int i = 0; i < Math.min(operationToIndex.length, 3); i++) {
                System.out.print("  工件" + i + ": ");
                for (int j = 0; j < operationToIndex[i].length; j++) {
                    System.out.print(operationToIndex[i][j] + " ");
                }
                System.out.println();
            }

            // 检查proDesMatrix
            double[][] proDesMatrix = problem.getProDesMatrix();
            System.out.println("proDesMatrix长度: " + proDesMatrix.length);
            for (int i = 0; i < Math.min(proDesMatrix.length, 10); i++) {
                System.out.print("  工序" + i + ": ");
                for (int j = 0; j < Math.min(proDesMatrix[i].length, 5); j++) {
                    System.out.print(proDesMatrix[i][j] + " ");
                }
                System.out.println();
            }

            System.out.println("\n开始运行GA...");
            GA ga = new GA(problem);
            ga.solve();

            System.out.println("运行完成！");

        } catch (Exception e) {
            System.err.println("\n❌ 运行出错:");
            System.err.println("错误类型: " + e.getClass().getSimpleName());
            System.err.println("错误信息: " + e.getMessage());
            System.err.println("\n详细堆栈:");
            e.printStackTrace();
        }
    }
}

